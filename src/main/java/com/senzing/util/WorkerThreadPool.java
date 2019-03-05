package com.senzing.util;

import java.util.*;

/**
 * Provides a simple worker thread that can be pooled and can execute a task
 * within the thread.
 */
public class WorkerThreadPool {
  /**
   * The list of available {@link WorkerThread} instances.
   */
  private List<WorkerThread> available;

  /**
   * The list of all {@link WorkerThread} instances whether available or not.
   */
  private List<WorkerThread> allThreads;

  /**
   * Flag indicating if the pool has been marked closed.
   */
  private boolean closed;

  /**
   * Constructs with the specified number of threads in the pool.
   *
   * @param count The number of threads to create in the pool.
   */
  public WorkerThreadPool(int count)
  {
    this("WorkerThread", count);
  }

  /**
   * Constructs with the specified thread base name and the number of threads
   * to create.
   *
   * @param baseName The base name to use as a prefix when naming the
   *                 worker threads in the pool.
   *
   * @param count The number of worker threads to create.
   */
  public WorkerThreadPool(String baseName, int count)
  {
    this.available    = new LinkedList<>();
    this.allThreads   = new LinkedList<>();
    this.closed       = false;

    // if baseName ends with "-" then strip it off since we will add it back
    if (baseName.endsWith("-")) {
      baseName = baseName.substring(0, baseName.length() - 1);
    }

    for (int index = 0; index < count; index++) {
      WorkerThread wt = new WorkerThread();
      wt.setName(baseName + "-" + index);
      this.available.add(wt);
      this.allThreads.add(wt);
      wt.start();
    }
  }

  /**
   * Checks if this pool has been closed.  Once closed, the pool can no longer
   * be used to execute any further tasks.
   */
  public boolean isClosed() {
    synchronized (this.available) {
      return this.closed;
    }
  }

  /**
   * Closes this pool so no further tasks can be executed against it.
   *
   * @param join Whether or not to join against each thread and wait for each
   *             thread to complete.
   */
  public void close(boolean join)
  {
    // mark this pool as closed and notify
    synchronized (this.available) {
      this.closed = true;
      this.available.notifyAll();
    }

    // mark all the threads complete
    for (WorkerThread thread: this.allThreads) {
      thread.markComplete();
    }

    // check if we are joining
    if (join) {
      // loop through the threads and join
      for (WorkerThread thread: this.allThreads) {
        try {
          // join against this thread
          thread.join();

        } catch (InterruptedException ignore) {
          // ignore the exception
        }
      }
    }
  }

  /**
   * Executes the specified task on the first available worker thread.
   *
   * @param task The {@link Task} to execute.
   *
   * @return The result from executing the {@link Task}.
   *
   * @throws Exception If the specified {@link Task#execute()} method throws
   *                   an exception.
   */
  public <T, E extends Exception> T execute(Task<T, E> task) throws E
  {
    WorkerThread thread = null;
    synchronized (this.available) {
      // check if already closed
      if (this.isClosed()) {
        throw new IllegalStateException(
            "This WorkerThreadPool has already been marked as closed and the "
            + "threads have been shutdown.");
      }

      // wait for an available worker thread
      while (this.available.size() == 0) {
        try {
          this.available.wait(2000L);
        } catch (InterruptedException ignore) {
          // do nothing
        }
      }
      thread = this.available.remove(0);
    }

    // execute the task on the thread and get the result
    try {
      // execute the task and return the result
      return thread.execute(task);

    } finally {
      if (thread != null) {
        // synchronize on the available pool
        synchronized (this.available) {
          // if not complete then return it to the pool and set thread to null
          if (!this.closed) {
            this.available.add(thread);
            this.available.notifyAll();

            // set the thread to null to indicate that it was returned
            // to the thread pool
            thread = null;
          }
        }

        // if the thread was not returned to the pool then we need to
        // mark it complete so it cleans up
        if (thread != null) {
          thread.markComplete();
        }
      }
    }
  }

  /**
   * The interface describing the tasks that can be performed by the
   * WorkerThread.
   */
  public interface Task<T, E extends Exception> {
    T execute() throws E;
  }

  /**
   * Internal worker thread class.
   */
  private class WorkerThread extends Thread {
    /**
     * Flag to indicate if the worker thread is complete.
     */
    private boolean complete;

    /**
     * The task to execute.
     */
    private Task task = null;

    /**
     * The returned object from the last task.
     */
    private Object result = null;

    /**
     * The failure from the last task.
     */
    private Exception failure = null;

    /**
     * Flag indicating if this thread is busy executing a task.
     */
    private boolean busy = false;

    /**
     * Default constructor.
     */
    private WorkerThread() {
      this.complete = false;
    }

    /**
     * Resets the thread so it is ready to execute the next task.
     */
    private void reset() {
      this.task = null;
      this.result = null;
      this.failure = null;
      this.busy = false;
    }

    /**
     * Checks if the thread has been marked complete and should stop
     * processing tasks.
     *
     * @return <tt>true</tt> if this instance has been marked complete,
     *         otherwise <tt>false</tt>.
     */
    private synchronized boolean isComplete() {
      return this.complete;
    }

    /**
     * Marks this thread complete so it stops waiting for new tasks to execute.
     */
    private synchronized void markComplete() {
      this.complete = true;
      this.notifyAll();
    }

    /**
     * Checks if this thread is currently busy executing a task.
     *
     * @return <tt>true</tt> if this thread is currently busy executing a
     *         task, otherwise <tt>false</tt>.
     */
    private synchronized boolean isBusy() {
      return this.busy;
    }

    /**
     * Executes the specified task and returns the result from that task or
     * throws the exception generated by the task.
     *
     * @param task The task to execute.
     *
     * @return The object return returned by the specified task.
     *
     * @throws Exception If the specified task throws an exception.
     */
    private synchronized <T, E extends Exception> T execute(Task<T, E> task)
        throws E
    {
      try {
        if (this.isBusy()) {
          throw new IllegalStateException("Already busy with another task.");
        }

        // flag as busy
        this.busy = true;

        // set the runnable
        this.task = task;

        // notify
        this.notifyAll();

        // wait for completion (releasing the lock)
        while (this.task != null) {
          try {
            this.wait(2000L);

          } catch (InterruptedException ignore) {
            // ignore the exception
          }
        }

        // check for a failure
        if (this.failure != null) {
          E e = (E) this.failure;
          this.reset();
          throw e;
        }

        // get the result
        T result = (T) this.result;

        // reset the worker thread
        this.reset();

        // return the result
        return result;

      } finally {
        this.reset();
      }
    }

    /**
     * Implement the run method to wait for the next task and execute it.
     * This continues until this thread is marked complete.
     */
    public void run()
    {
      synchronized (this) {
        // loop while not complete
        while (!this.isComplete()) {

          // loop while not complete and no task
          while (this.task == null && !this.isComplete()) {
            try {
              this.wait(10000L);
            } catch (InterruptedException ignore) {
              // do nothing
            }
          }

          // check if we have a task to do
          if (this.task != null) {
            try {
              // execute the task and record the result
              this.result = this.task.execute();

            } catch (Exception e) {
              // record any failure for the task
              this.failure = e;
            }
            // clear the task
            this.task = null;

            // make sure to notify when done
            this.notifyAll();
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    WorkerThreadPool pool = new WorkerThreadPool(4);
    for (int index = 0; index < args.length; index++) {
      final String arg = args[index];
      try {
        Object result = pool.execute(() -> {
          String threadName = Thread.currentThread().getName();
          String message = threadName + ": " + arg;
          if (arg.startsWith("ERROR")) {
            throw new RuntimeException(message);
          }
          return message;
        });

        System.out.println(result);

      } catch (Exception failure) {
        failure.printStackTrace();
      }
    }
    System.out.println();
    System.out.println("JOINING AGAINST THE POOL....");
    pool.close(true);
    System.out.println("JOINED AGAINST THE POOL.");
  }
}