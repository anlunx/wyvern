package wyvern.stdlib.support;

/* Imports */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/* Future -> CompletableFuture conversion as described in 
http://www.thedevpiece.com/converting-old-java-future-to-completablefuture/ */
class CompletablePromiseContext {
    private static final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor();
    
    public static void schedule(Runnable r) {
        SERVICE.schedule(r, 1, TimeUnit.MILLISECONDS);
    }
}

class CompletablePromise<V> extends CompletableFuture<V> {
    private Future<V> future;
    
    public CompletablePromise(Future<V> future) {
        this.future = future;
        CompletablePromiseContext.schedule(this::tryToComplete);
    }
    
    private void tryToComplete() {
        if (future.isDone()) {
            try {
                complete(future.get());
            } catch (InterruptedException e) {
                completeExceptionally(e);
            } catch (ExecutionException e) {
                completeExceptionally(e.getCause());
            }
            return;
        }
        if (future.isCancelled()) {
            cancel(true);
            return;
        }
        //continue to loop
        CompletablePromiseContext.schedule(this::tryToComplete);
    }
}


public class NIO {
    public static final NIO nio = new NIO();
    public NIO() { }
    
    public AsynchronousServerSocketChannel makeServerSocketChannel(int port) throws IOException {
        AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(port));
        return serverSocket;
    }
    
    public AsynchronousSocketChannel makeSocketChannel() throws IOException {
        AsynchronousSocketChannel socket = AsynchronousSocketChannel.open();
        //socket.connect(new InetSocketAddress(hostname, port));
        return socket;
    }
    
    public Future<Void> socketChannelConnect(Object sc, String hostname, int port) {
        AsynchronousSocketChannel chan = (AsynchronousSocketChannel) sc;
        return chan.connect(new InetSocketAddress(hostname, port));
    }
    
    public Future<AsynchronousSocketChannel> serverAccept(Object server) {
        AsynchronousServerSocketChannel s = (AsynchronousServerSocketChannel) server;
        return s.accept();
    }
    
    /* initializes a buffer to pass to Wyvern */
    public ByteBuffer initBuffer(int capacity) {
        return ByteBuffer.allocate(capacity);
    }
    
    /** Async TCP socket methods **/
    
    /* asynchronously reads from socket channel (chan) into byte buffer (b) */
    public Future<Integer> socketRead(Object chan, Object b) {
        AsynchronousSocketChannel socket = (AsynchronousSocketChannel) chan;
        ByteBuffer buf = (ByteBuffer) b;
        Future<Integer> status = socket.read(buf);
        return status;
    }
    
    public Future<Integer> socketWrite(Object chan, Object b) {
        AsynchronousSocketChannel socket = (AsynchronousSocketChannel) chan;
        ByteBuffer buf = (ByteBuffer) b;
        Future<Integer> status = socket.write(buf);
        return status;
    }
    
    public void socketClose(Object chan) throws IOException {
        AsynchronousSocketChannel socket = (AsynchronousSocketChannel) chan;
        socket.close();
    }
    
    /** Future wrapper **/
    public boolean futureIsCancelled(Object obj) {
        Future f = (Future) obj;
        return f.isCancelled();
    }
    
    public boolean futureIsDone(Object obj) {
        Future f = (Future) obj;
        return f.isDone();
    }
    
    public <T> T futureGet(Object obj) {
        Future<T> f = (Future<T>) obj;
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException ex) {
            return null; //might need to change later
        }
    }
    
    public boolean futureCancel(Object obj) {
        Future f = (Future) obj;
        return f.cancel(true);
    }
    
    public <T> Future<T> futureNew(T value) {
        return CompletableFuture.completedFuture(value);
    }
    /*
    public <T,U> Function<T,U> wrapWyvernFunction(Object fn) {
        return () -> fn;
    }
    */
    //doesn't work with casting
    public <T,U> CompletableFuture<U> applyCallback(Object obj, Object fn) {
        Future<T> f = (Future<T>) obj;
        CompletableFuture<T> completableFuture = new CompletablePromise<T>(f);
        Function<T,U> fun = (Function<T,U>) fn; //need to wrap this
        return completableFuture.thenApply(fun);
    }
	/*
	function from T to wyvern T, apply wyvern function, return
	*/
	

    /** ByteBuffer abstraction **/
    public ByteBuffer makeByteBuffer(int allocSize) {
        return ByteBuffer.allocate(allocSize);
    }
    
    public int byteBufferGet(Object buf, int index) {
        ByteBuffer b = (ByteBuffer) buf;
        return b.get(index);
    }
    
    //note that this puts an int
    public void byteBufferSet(Object buf, int index, int value) {
        ByteBuffer b = (ByteBuffer) buf;
        b.putInt(index, value);
    }
    
}