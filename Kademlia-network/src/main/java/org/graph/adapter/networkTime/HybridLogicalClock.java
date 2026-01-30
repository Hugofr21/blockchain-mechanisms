package org.graph.adapter.networkTime;

import java.io.Serial;
import java.io.Serializable;

public class HybridLogicalClock implements Comparable<HybridLogicalClock>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long physicalClock;
    private int logicalClock;

    public HybridLogicalClock() {
        this.physicalClock = (System.currentTimeMillis() /1000);
        this.logicalClock = 0;
    }

    private HybridLogicalClock(long physicalClock, int logicalClock) {
        this.physicalClock = physicalClock;
        this.logicalClock = logicalClock;
    }

    public long getPhysicalClock() {
        return physicalClock;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public synchronized HybridLogicalClock next() {
        long now = System.currentTimeMillis();
        if (now > physicalClock) {
            physicalClock = now;
            logicalClock=0;
        }else {
            logicalClock++;
        }
        return this;
    }

    public synchronized void update(HybridLogicalClock remote) {
        long now = System.currentTimeMillis();
        long maxPhysical = Math.max(Math.max(now, this.physicalClock), remote.physicalClock);

        if (maxPhysical == this.physicalClock && maxPhysical == remote.physicalClock) {
            this.logicalClock = Math.max(this.logicalClock, remote.logicalClock) + 1;
        } else if (maxPhysical == this.physicalClock) {
            this.logicalClock = Math.max(this.logicalClock, remote.logicalClock) + 1;
        } else if (maxPhysical == remote.physicalClock) {
            this.logicalClock = Math.max(this.logicalClock, remote.logicalClock) + 1;
        } else {
            this.logicalClock = 0;
        }

        this.physicalClock = maxPhysical;

    }

    @Override
    public int compareTo(HybridLogicalClock o) {
        int cmp = Long.compare(this.physicalClock, o.physicalClock);
        if (cmp == 0) {
            cmp = Long.compare(this.logicalClock, o.logicalClock);
        }
        return cmp;
    }


    public static HybridLogicalClock fromString(String s) {
        String[] parts = s.split("_");
        long p = Long.parseLong(parts[0]);
        int   l = Integer.parseInt(parts[1]);
        return new HybridLogicalClock(p, l);
    }
}
