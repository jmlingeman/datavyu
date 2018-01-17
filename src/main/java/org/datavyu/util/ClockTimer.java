/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * ClockTime is a class which can be used as a time to keep multiple objects in sync.
 */
public final class ClockTimer {

    /** Syncronization threshold in milliseconds */
    public static final long SYNC_THRESHOLD = 50L;

    /** Clock tick period in milliseconds */
    private static final long CLOCK_SYNC_INTERVAL = 2000L;

    /** Clock initial delay in milliseconds */
    private static final long CLOCK_SYNC_DELAY = 0L;

    /** Convert nanoseconds to milliseconds */
    private static final long NANO_IN_MILLI = 1000000L;

    /** Current time of the clock */
    private double elapsedTime;

    /** Used to calculate elapsed time */
    private double lastTime;

    /** Is the clock currently stopped */
    private boolean isStopped;

    /** Update multiplier */
    private float rate = 1F;

    /** The set of objects that listen to this clock */
    private Set<ClockListener> clockListeners = new HashSet<>();

    /**
     * Default constructor.
     */
    public ClockTimer() {
        elapsedTime = 0;
        lastTime = 0;
        isStopped = true;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sync();
            }
        }, CLOCK_SYNC_DELAY, CLOCK_SYNC_INTERVAL);
    }

    public synchronized double getElapsedTime() {
        return elapsedTime;
    }

    /**
     * @return Current clock rate.
     */
    public synchronized float getRate() {
        return rate;
    }

    /**
     * @param newRate Multiplier for CLOCK_TICK.
     */
    public synchronized void setRate(float newRate) {
        updateElapsedTime();
        rate = newRate;
        notifyRate();
    }

    /**
     * Initiate starting of clock.
     */
    public synchronized void start() {
        if (isStopped) {
            isStopped = false;
            lastTime = System.nanoTime();
            notifyStart();
        }
    }

    /**
     * Set flag to stop clock at next time update (boundary).
     */
    public synchronized void stop() {
        if (!isStopped) {
            updateElapsedTime();
            isStopped = true;
            notifyStop();
        }
    }

    /**
     * @return True if clock is stopped.
     */
    public synchronized boolean isStopped() {
        return isStopped;
    }

    /**
     * @param listener Listener requiring clockTick updates.
     */
    public synchronized void registerListener(final ClockListener listener) {
        clockListeners.add(listener);
    }

    private synchronized void updateElapsedTime() {
        double newTime = System.nanoTime();
        elapsedTime += isStopped ? 0 : rate * (newTime - lastTime) / NANO_IN_MILLI;
        lastTime = newTime;
    }

    /**
     * The "sync" of the clock - updates listeners of changes in time.
     */
    private synchronized void sync() {
        if (!isStopped) {
            updateElapsedTime();
            notifySync();
        }
    }

    /**
     * Notify clock listeners of sync.
     */
    private void notifySync() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockSync(elapsedTime);
        }
    }

    /**
     * Notify clock listeners of rate update.
     */
    private void notifyRate() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockRate(rate);
        }
    }

    /**
     * Notify clock listeners of start event.
     */
    private void notifyStart() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockStart(elapsedTime);
        }
    }

    /**
     * Notify clock listeners of stop event.
     */
    private void notifyStop() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockStop(elapsedTime);
        }
    }

    /**
     * Listener interface for clock 'ticks'.
     */
    public interface ClockListener {

        /**
         * @param elapsedTime Current time in milliseconds
         */
        void clockSync(double elapsedTime);

        /**
         * @param elapsedTime Current time in milliseconds
         */
        void clockStart(double elapsedTime);

        /**
         * @param elapsedTime Current time in milliseconds
         */
        void clockStop(double elapsedTime);

        /**
         * @param rate Current (updated) rate.
         */
        void clockRate(float rate);
    }
}
