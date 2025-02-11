package com.lwq.reentrant;

/**
 * 为每个锁关联一个请求计数器和一个占有它的线程。
 * 当计数为0时，认为锁是未被占有的；
 * 线程请求一个未被占有的锁时，JVM将记录锁的占有者，并且将请求计数器置为1 。
 */
public class ReentrantLock {

    boolean isLocked = false;
    Thread lockBy = null; // 独占线程
    int lockedCount = 0; // 计数器

    public synchronized void lock() throws InterruptedException {
        Thread thread = Thread.currentThread();
        while (isLocked && lockBy != thread) { // 判断加锁，而且线程不是当前线程
            wait();//已经加锁了，而且加锁的不是你，那你就在这阻塞吧，一直自旋，等人家释放了，你就出去，出去把那些信号变量都变成你自己的
        }
        isLocked = true;
        lockedCount++; // 计数器 +1
        lockBy = thread;
    }

    public synchronized void unlock() {
        if (Thread.currentThread() == this.lockBy) { // 判断是否是当前线程
            lockedCount--;
            if (lockedCount == 0) {  // 计数器为0时，释放锁
                isLocked = false;
                System.err.println(lockBy);
                notify();
            }
        }
    }
}