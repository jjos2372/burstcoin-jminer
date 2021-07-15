package burstcoin.jminer.core.checker.util;

import pocminer.generate.MiningPlot;
import signumj.util.LibShabal;

public class ShaLibChecker {

    public ShaLibChecker() {
    }
    
    public Throwable getLoadError() {
      return LibShabal.LOAD_ERROR;
    }

    public int findLowest(byte[] gensig, byte[] data) {
        return (int) LibShabal.shabal_findBestDeadline(data, data.length / MiningPlot.SCOOP_SIZE, gensig);
    }
}
