package org.jboss.unimbus.test;

import javax.enterprise.inject.spi.Unmanaged;

import org.jboss.unimbus.UNimbus;
import org.jboss.unimbus.test.impl.EphemeralPortsConfigSource;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * JUnit test-runner.
 *
 * <p>May be used to execute a test class as an injectable component when used with {@link RunWith}</p>
 *
 * <pre>
 *     &#64;RunWith(UNimbusTestRunner.class)
 *     public class MyTest {
 *
 *         &#64;Test
 *         public void testSomething() throws Exception {
 *             ...
 *         }
 *
 *         &#64;Inject
 *         private MyComponent myComponent;
 *
 *     }
 * </pre>
 */
public class UNimbusTestRunner extends BlockJUnit4ClassRunner {

    public UNimbusTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Object createTest() throws Exception {
        Unmanaged unmanaged = new Unmanaged(getTestClass().getJavaClass());
        this.instance = unmanaged.newInstance().produce().inject().postConstruct();
        return instance.get();
    }

    @Override
    public void run(RunNotifier notifier) {
        configureEphemeralPorts();
        try {
            this.system = new UNimbus(getTestClass().getJavaClass()).start();
            super.run(notifier);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if ( this.instance != null ) {
                this.instance.preDestroy().dispose();
                this.instance = null;
            }
            EphemeralPortsConfigSource.INSTANCE.reset();
            if (this.system != null) {
                this.system.stop();
                this.system = null;
            }
        }
    }

    private void configureEphemeralPorts() {
        EphemeralPortsConfigSource.INSTANCE.reset();
        EphemeralPorts anno = getTestClass().getJavaClass().getAnnotation(EphemeralPorts.class);
        if (anno != null) {
            EphemeralPortsConfigSource.INSTANCE.setPrimaryIsEphemeral(anno.primary());
            EphemeralPortsConfigSource.INSTANCE.setManagementIsEphemeral(anno.management());
        }
    }

    private UNimbus system;

    private Unmanaged.UnmanagedInstance instance;
}
