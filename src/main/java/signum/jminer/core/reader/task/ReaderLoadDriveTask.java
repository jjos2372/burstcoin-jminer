/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 by luxe - https://github.com/de-luxe - BURST-LUXE-RED2-G6JW-H4HG5
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package signum.jminer.core.reader.task;


import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jna.Platform;

import signum.jminer.core.CoreProperties;
import signum.jminer.core.checker.event.CheckerResultEvent;
import signum.jminer.core.checker.util.ShaLibChecker;
import signum.jminer.core.reader.Reader;
import signum.jminer.core.reader.data.PlotDrive;
import signum.jminer.core.reader.data.PlotFile;
import signum.jminer.core.reader.event.ReaderDriveFinishEvent;
import signum.jminer.core.reader.event.ReaderDriveInterruptedEvent;
import signum.jminer.core.reader.event.ReaderLoadedPartEvent;
import net.smacke.jaydio.DirectRandomAccessFile;
import signumj.crypto.plot.impl.MiningPlot;


/**
 * Executed once for every block ... reads scoops of drive plots
 */
@Component
@Scope("prototype")
public class ReaderLoadDriveTask
  implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(ReaderLoadDriveTask.class);

  private final ApplicationEventPublisher publisher;
  private ShaLibChecker shaLibChecker;

  private byte[] generationSignature;
  private PlotDrive plotDrive;
  private int[] scoopNumber;
  private long blockNumber;
  private boolean showDriveInfo;

  @Autowired
  public ReaderLoadDriveTask(ApplicationEventPublisher publisher)
  {
    this.publisher = publisher;
  }

  public void init(int[] scoopNumber, long blockNumber, byte[] generationSignature, PlotDrive plotDrive)
  {
    this.scoopNumber = scoopNumber;
    this.blockNumber = blockNumber;
    this.generationSignature = generationSignature;
    this.plotDrive = plotDrive;

    showDriveInfo = CoreProperties.isShowDriveInfo();

    if(!CoreProperties.isUseOpenCl())
    {
      this.shaLibChecker = new ShaLibChecker();
    }
  }

  @Override
  public void run()
  {
    long startTime = showDriveInfo ? new Date().getTime() : 0;
    Iterator<PlotFile> iterator = plotDrive.getPlotFiles().iterator();
    boolean interrupted = false;
    while(iterator.hasNext() && !interrupted)
    {
      PlotFile plotPathInfo = iterator.next();
      if(plotPathInfo.getStaggeramt() % plotPathInfo.getNumberOfParts() > 0)
      {
        LOG.warn("staggeramt " + plotPathInfo.getStaggeramt() + " can not be devided by " + plotPathInfo.getNumberOfParts());
        // fallback ... could lead to problems on optimized plot-files
        plotPathInfo.setNumberOfParts(1);
      }
      interrupted = load(plotPathInfo);
    }

    if(showDriveInfo)
    {
      if(interrupted)
      {
        // ui-event
        publisher.publishEvent(new ReaderDriveInterruptedEvent(blockNumber, plotDrive.getDirectory()));
      }
      else
      {
        // ui event
        publisher.publishEvent(new ReaderDriveFinishEvent(plotDrive.getDirectory(), plotDrive.getSize(), new Date().getTime() - startTime, blockNumber));
      }
    }
  }

  /**
   * A wrapper class optimized for each system.
   * 
   * On Linux the page cache keeps being filled by the plots recently read,
   * so we use direct io in this case.
   * 
   * TODO: add optimized support for other platforms.
   *
   */
  static class RandomAccessFileWrapper implements Closeable {
    DirectRandomAccessFile dra;
    RandomAccessFile ra;
    
    public RandomAccessFileWrapper(Path path) throws IOException {
      if(Platform.isLinux())
        dra = new DirectRandomAccessFile(path.toFile(), "r");
      else
        ra = new RandomAccessFile(path.toFile(), "r");
    }

    @Override
    public void close() throws IOException {
      if(dra != null)
        dra.close();
      if(ra != null)
        ra.close();
    }

    public void seek(long l) throws IOException {
      if(dra!=null)
        dra.seek(l);
      if(ra!=null)
        ra.seek(l);
    }

    public void read(byte[] partBuffer, int i, int length) throws IOException {
      if(dra!=null)
        dra.read(partBuffer, i, length);
      if(ra!=null)
        ra.read(partBuffer, i, length);      
    }
  };
  
  private boolean load(PlotFile plotFile)
  {
    try (RandomAccessFileWrapper sbc = new RandomAccessFileWrapper(plotFile.getFilePath())) {
      
      long currentScoopPosition = scoopNumber[0] * plotFile.getStaggeramt() * MiningPlot.SCOOP_SIZE;
      
      long partSize = plotFile.getStaggeramt() / plotFile.getNumberOfParts();
      byte []partBuffer = new byte[(int) (partSize * MiningPlot.SCOOP_SIZE)];
      // optimized plotFiles only have one chunk!
      for(int chunkNumber = 0; chunkNumber < plotFile.getNumberOfChunks(); chunkNumber++)
      {
        long currentChunkPosition = chunkNumber * plotFile.getStaggeramt() * MiningPlot.PLOT_SIZE;
        sbc.seek(currentScoopPosition + currentChunkPosition);
                
        for(int partNumber = 0; partNumber < plotFile.getNumberOfParts(); partNumber++)
        {
          sbc.read(partBuffer, 0, partBuffer.length);

          if(Reader.blockNumber.get() != blockNumber || !Arrays.equals(Reader.generationSignature, generationSignature))
          {
            LOG.trace("loadDriveThread stopped!");
            sbc.close();
            return true;
          }
          else
          {
            BigInteger chunkPartStartNonce = plotFile.getStartnonce().add(BigInteger.valueOf(chunkNumber * plotFile.getStaggeramt() + partNumber * partSize));
            final byte[] scoops = partBuffer;
            publisher.publishEvent(new ReaderLoadedPartEvent(blockNumber, generationSignature, scoops, chunkPartStartNonce, plotFile.getFilePath().toString()));

            if(!CoreProperties.isUseOpenCl() && shaLibChecker.getLoadError() == null)
            {
              int lowestNonce = shaLibChecker.findLowest(generationSignature, scoops);
              publisher.publishEvent(new CheckerResultEvent(blockNumber, generationSignature, chunkPartStartNonce, lowestNonce,
                                                            plotFile.getFilePath().toString(), scoops));
            }
          }
        }
      }
      sbc.close();
    }
    catch(NoSuchFileException exception)
    {
      LOG.error("File not found ... please restart to rescan plot-files, maybe set rescan to 'true': " + exception.getMessage());
    }
    catch(ClosedByInterruptException e)
    {
      // we reach this, if we do not wait for task on shutdown - ByteChannel closed by thread interruption
      LOG.trace("reader stopped cause of new block ...");
    }
    catch(IOException e)
    {
      LOG.error("IOException in: " + plotFile.getFilePath().toString() + " -> " + e.getMessage());
    }
    return false;
  }
}
