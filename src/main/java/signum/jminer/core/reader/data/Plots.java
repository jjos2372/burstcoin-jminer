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

package signum.jminer.core.reader.data;


import signum.jminer.core.CoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Plots
{
  private static final Logger LOG = LoggerFactory.getLogger(Plots.class);

  private Collection<PlotDrive> plotDrives;
  private Map<BigInteger, Long> chunkPartStartNonces;

  public Plots(String numericAccountId)
  {
    plotDrives = new HashSet<>();
    chunkPartStartNonces = new HashMap<>();
    Map<String, Collection<Path>> plotFilesLookup = collectPlotFiles(CoreProperties.getPlotPaths(), numericAccountId);
    for(Map.Entry<String, Collection<Path>> entry : plotFilesLookup.entrySet())
    {
      PlotDrive plotDrive = new PlotDrive(entry.getKey(), entry.getValue(), CoreProperties.getChunkPartNonces());
      if(!plotDrive.getPlotFiles().isEmpty())
      {
        plotDrives.add(plotDrive);

        int expectedSize = chunkPartStartNonces.size() + plotDrive.collectChunkPartStartNonces().size();
        chunkPartStartNonces.putAll(plotDrive.collectChunkPartStartNonces());
        if(expectedSize != chunkPartStartNonces.size())
        {
          LOG.error("possible duplicate/overlapping plot-file on drive '" + plotDrive.getDirectory() + "' please check your plots.");
        }
      }
      else
      {
        LOG.info("No plotfiles found at '" + plotDrive.getDirectory() + "' ... will be ignored.");
      }
    }
  }

  public Collection<PlotDrive> getPlotDrives()
  {
    return plotDrives;
  }

  private static Map<String, Collection<Path>> collectPlotFiles(List<String> plotDirectories, String numericAccountId)
  {
    Map<String, Collection<Path>> plotFilesLookup = new HashMap<>();
    for(String plotDirectory : plotDirectories)
    {
      Path folderPath = Paths.get(plotDirectory);
      try (DirectoryStream<Path> plotFilesStream = Files.newDirectoryStream(folderPath))
      {
        List<Path> plotFilePaths = new ArrayList<>();
        for(Path plotFilePath : plotFilesStream)
        {
          if(plotFilePath.toString().contains(numericAccountId))
          {
            plotFilePaths.add(plotFilePath);
          }
        }
        plotFilesLookup.put(plotDirectory, plotFilePaths);
      }
      catch(IOException | DirectoryIteratorException e)
      {
        LOG.error(e.getMessage());
      }
    }
    return plotFilesLookup;
  }

  /* total number of bytes of all plotFiles */
  public long getSize()
  {
    long size = 0;
    for(PlotDrive plotDrive : plotDrives)
    {
      size += plotDrive.getSize();
    }
    return size;
  }

  public void printPlotFiles()
  {
    for(PlotDrive plotDrive : getPlotDrives())
    {
      for(PlotFile plotFile : plotDrive.getPlotFiles())
      {
        System.out.println(plotFile.getFilePath());
      }
    }
  }

  /* gets plot file by plot file start nonce. */
  public PlotFile getPlotFileByPlotFileStartNonce(long plotFileStartNonce)
  {
    for(PlotDrive plotDrive : getPlotDrives())
    {
      for(PlotFile plotFile : plotDrive.getPlotFiles())
      {
        if(plotFile.getFilename().contains(String.valueOf(plotFileStartNonce)))
        {
          return plotFile;
        }
      }
    }
    return null;
  }

  /* gets chunk part start nonces. */
  public Map<BigInteger, Long> getChunkPartStartNonces()
  {
    return chunkPartStartNonces;
  }

  /* gets plot file by chunk part start nonce. */
  public PlotFile getPlotFileByChunkPartStartNonce(BigInteger chunkPartStartNonce)
  {
    for(PlotDrive plotDrive : getPlotDrives())
    {
      for(PlotFile plotFile : plotDrive.getPlotFiles())
      {
        if(plotFile.getChunkPartStartNonces().containsKey(chunkPartStartNonce))
        {
          return plotFile;
        }
      }
    }
    return null;
  }
}
