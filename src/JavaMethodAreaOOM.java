import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * VM Args：-XX:MetaspaceSize=10M -XX:MaxMetaspaceSize=10M
 * 
 * cglib-nodep 里包含 asm 包，cglib 里不包含asm 包，用 cglib-nodep 就不会出现版本不匹配情况
 * 
 * @author moonspirit
 * @version 1.0
 */
public class JavaMethodAreaOOM {

	public static void main(String[] args) {
		while (true) {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(OOMObject.class);
			enhancer.setUseCache(false);
			enhancer.setCallback(new MethodInterceptor() {
				public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
					return proxy.invokeSuper(obj, args);
				}
			});
			enhancer.create();
		}
	}

	static class OOMObject {
	}
}
