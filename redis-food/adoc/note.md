# redisfood项目中遇到问题的总结理解

## 1、关于为何不加锁就会出现线程不安全的问题

~~~java
public class RedisLockTest {
    private int count = 0;
    private void call(Jedis jedis) {
        for (int i = 0; i < 500; i++) {
            //线程到达A
            count++;
        }
    }
    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> redisLockTest.call(jedisPool.getResource()));
        Thread t2 = new Thread(() -> redisLockTest.call(jedisPool.getResource()));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(redisLockTest.count);
    }
}
for (int i = 0; i < 500; i++) {
    count++;
}
~~~

~~~markdown
这段代码被两个线程调用，功能就是为了给count做加一，没个线程运行500次，也就是做500次加法，正常应该是1000，但是实际却不是，因为这涉及到jmm内存模型。
我来解释一下,这里只做粗略解释，后面juc的时候详细说。
两个线程各自有自己的临时内存，我们比如两个线程一起到达A处，此时他们两个读取的count都是0，此时第一个线程往下走，各种加，把count加到了200，但是此时还没写回主内存，只是在自己的临时内存，然后他写回主存，count是200。第二个线程可能卡了一下，等他要执行他的临时内存此时存的count是0，他就从0开始加，加到100，他也写回主存，就把刚才线程1写的200，覆盖了，这样就是数据污染，然后他们这样因为并发，交替处理数据，导致最后没有达到1000，因为不同线程之间会覆盖数据，导致做无用功。线程的不可控制出现这一原因，所以需要加锁，每次执行只能有一个线程进来。谁也不会读到不同的临时数据，进来读的就是上一个线程改完的，按顺序走。
~~~

