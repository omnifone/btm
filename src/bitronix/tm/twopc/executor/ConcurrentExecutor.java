package bitronix.tm.twopc.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import bitronix.tm.internal.BitronixRuntimeException;

/**
 * Abstraction of the <code>java.util.concurrent</code> JDK 1.5+ implementation.
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class ConcurrentExecutor implements Executor {

    private final static Logger log = LoggerFactory.getLogger(ConcurrentExecutor.class);

    private final static String[] implementations = {
        "java.util.concurrent.Executors",
        "java.util.concurrent.ExecutorService",
        "java.util.concurrent.Future",
        "java.util.concurrent.TimeUnit"
    };


    private Object executorService;
    private Method executorServiceSubmitMethod;
    private Method executorServiceShutdownMethod;
    private Method futureGetMethod;
    private Method futureIsDoneMethod;
    private Object timeUnitMilliseconds;
    private boolean usable = false;


    public ConcurrentExecutor() {
        this(implementations);
    }

    protected ConcurrentExecutor(String[] implementations) {
        init(implementations);
    }

    private void init(String[] implementations) {
        String executorsImpl = implementations[0];
        String executorServiceImpl = implementations[1];
        String futureImpl = implementations[2];
        String timeUnitImpl = implementations[3];
        if (log.isDebugEnabled()) log.debug("initializing concurrent executor implementation <" + executorsImpl + ">");

        try {
            Class executorsClass = Thread.currentThread().getContextClassLoader().loadClass(executorsImpl);
            Class executorServiceClass = Thread.currentThread().getContextClassLoader().loadClass(executorServiceImpl);
            Class timeUnitClass = Thread.currentThread().getContextClassLoader().loadClass(timeUnitImpl);

            executorService = executorsClass.getMethod("newCachedThreadPool", (Class[]) null).invoke(executorsClass, (Object[]) null);
            executorServiceSubmitMethod = executorServiceClass.getMethod("submit", new Class[] { Runnable.class });
            executorServiceShutdownMethod = executorServiceClass.getMethod("shutdownNow", (Class[]) null);
            timeUnitMilliseconds = timeUnitClass.getField("MILLISECONDS").get(timeUnitClass);
            futureGetMethod = Thread.currentThread().getContextClassLoader().loadClass(futureImpl).getMethod("get", new Class[] { long.class, timeUnitClass });
            futureIsDoneMethod = Thread.currentThread().getContextClassLoader().loadClass(futureImpl).getMethod("isDone", (Class[]) null);

            if (log.isDebugEnabled()) log.debug("found a valid implementation for this executor <" + executorsImpl + ">");
            usable = true;
        } catch (Exception ex) {
            if (log.isDebugEnabled()) log.debug("error accessing executor implementation <" + executorsImpl + ">", ex);
        }

        if (!usable)
            if (log.isDebugEnabled()) log.debug("cannot find a valid implementation for executor <" + executorsImpl + ">, disabling it");
    }

    public Object submit(Runnable job) {
        if (!isUsable())
            throw new BitronixRuntimeException("concurrent executor is disabled because there is no valid executor implementation");
        try {
            return executorServiceSubmitMethod.invoke(executorService, new Object[] { job });
        } catch (IllegalAccessException ex) {
            throw new BitronixRuntimeException("error calling ExecutorService.submit(Runnable task)", ex);
        } catch (InvocationTargetException ex) {
            throw new BitronixRuntimeException("error calling ExecutorService.submit(Runnable task)", ex);
        }
    }

    public void waitFor(Object future, long timeout) {
        if (!isUsable())
            throw new BitronixRuntimeException("concurrent executor is disabled because there is no valid executor implementation");
        try {
            futureGetMethod.invoke(future, new Object[] { new Long(timeout), timeUnitMilliseconds});
        } catch (IllegalAccessException ex) {
            throw new BitronixRuntimeException("error calling Future.get()", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getCause().getClass().getName().endsWith("TimeoutException"))
                return; // wait timed out, simply return
            throw new BitronixRuntimeException("error calling Future.get()", ex);
        }
    }

    public boolean isDone(Object future) {
        if (!isUsable())
            throw new BitronixRuntimeException("concurrent executor is disabled because there is no valid executor implementation");
        try {
            Boolean b = (Boolean) futureIsDoneMethod.invoke(future, (Object[]) null);
            return b.booleanValue();
        } catch (IllegalAccessException ex) {
            throw new BitronixRuntimeException("error calling Future.isDone()", ex);
        } catch (InvocationTargetException ex) {
            throw new BitronixRuntimeException("error calling Future.isDone()", ex);
        }
    }

    public boolean isUsable() {
        return usable;
    }

    public void shutdown() {
        if (!isUsable())
            throw new BitronixRuntimeException("concurrent executor is disabled because there is no valid executor implementation");
        try {
            executorServiceShutdownMethod.invoke(executorService, (Object[]) null);
        } catch (IllegalAccessException ex) {
            throw new BitronixRuntimeException("error calling ExecutorService.shutdown()", ex);
        } catch (InvocationTargetException ex) {
            throw new BitronixRuntimeException("error calling ExecutorService.shutdown()", ex);
        }
    }
}