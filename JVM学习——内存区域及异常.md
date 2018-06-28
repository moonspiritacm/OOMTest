# 1. JVM 内存异常

JVM 自动内存管理机制包括内存动态分配和垃圾自动收集两部分，可能出现的内存异常包括`内存泄漏`和`内存溢出`两种。

- `内存泄漏`：已申请的内存空间无法被主动释放或及时回收，导致可用内存越来越少，最终造成内存溢出。

- `内存溢出`：空闲内存不足，无法分配新内存给程序，产生原因包括内存短缺（硬件、系统和虚拟机层面的内存限制）、内存需求过大（深度递归、大规模数据库读取）、内存泄漏等。

> 硬件级内存限制：物理内存与交换区空间总和。

> 系统级内存限制：处理器/操作系统位数限制内存寻址空间，32位操作系统寻址空间为2GB。

> 虚拟机级内存限制：`-Xms`、`-Xmx` 等参数设置 JVM 各内存区域大小。

# 2. JVM 内存区域划分

| 内存区域 | 属性 | 数据结构 | 存储内容 | 抛出异常类型 | 垃圾收集 |
| :-: | :-: | :-: | :-: | :-: |
| 程序计数器 | 线程私有 | 无 | 正在执行的字节码行号 | 无（唯一无异常抛出） | 无 |
| Java 虚拟机栈 | 线程私有 | 栈 | Java 方法栈帧 | StackOverflowError / OutOfMemoryError | 无 |
| 本地方法栈 | 线程私有 | 栈 | Native 方法栈帧 | StackOverflowError / OutOfMemoryError | 无 |
| Java 堆 | 线程共享 | 堆 | 对象实例 / 数组 | OutOfMemoryError | 有 |
| 方法区 | 线程共享 | 堆 | 类信息 / 常量 / 静态变量/ 即时编译代码 | OutOfMemoryError | 有 |


- Java 多线程是通过线程轮流切换并分配处理器执行时间的方式来实现的。程序计数器是实现分支、循环、跳转、异常处理和线程切换与恢复的重要结构。

- Sun HotSpot 虚拟机实现过程中将 Java 虚拟机栈与本地方法栈合二为一，统一管理。

- `String.intern()` 方法可以实现运行时将新常量加入运行时常量池。

`栈帧`是 JVM 方法执行的内存模型，包含局部变量表、操作数栈、动态链接、出口等部分，栈帧从入栈到出栈对应方法从调用到执行完成的过程。

`线程私有`内存区域随线程创建和撤销而被分配和收集，线程间互不影响，独立存储。

`线程共享`内存区域在虚拟机启动时创建，关闭时撤销，各线程共享内存空间，为多线程共享数据提供可能，同时存在线程同步问题。

`直接内存`不属于 JVM 内存区域，在 NIO 等场合应用，受到硬件及系统级内存限制，仍然可能出现 OutOfMemoryError 异常。

# 3. 内存异常验证

GitHub 源代码地址 [https://github.com/moonspiritacm/OOMTest]。

## 3.1 Java 堆溢出

### 3.1.1 产生原因

1. 内存溢出：JVM 分配的 Java 堆空间不足，程序创建的对象实例过多。

2. 内存泄漏：GC 根与无用对象之间存在可达路径导致无法被垃圾收集机制回收。

`-Xms` Java 堆初始分配内存大小，`-Xmx` Java 堆最大分配内存大小，二者相等表示禁止堆空间自动扩展。

`-XX:+HeapDumpOnOutOfMemoryError` 堆溢出时保存快照便于事后分析。

### 3.1.2 解决方案

1. 通过内存映像分析工具对堆转储快照进行分析，异常产生原因。

2. 对于内存泄漏，通过工具查看泄漏对象到 GC 根的可达路径，定位泄漏代码位置。

3. 对于内存溢出，一方面尽量调大堆空间，一方面修改代码尝试减少程序中对象的生命周期。

### 3.1.3 验证代码

```java
import java.util.ArrayList;
import java.util.List;

/**
 * VM Args：-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 * 
 * @author moonspirit
 * @version 1.0
 */
public class HeapOOM {

	static class OOMObject {
	}

	public static void main(String[] args) {
		List<OOMObject> list = new ArrayList<OOMObject>();

		while (true) {
			list.add(new OOMObject());
		}
	}
}
```

## 3.2 虚拟机栈和本地方法栈溢出（单线程）

单线程下，虚拟机栈异常只抛出 StackOverflowError。

### 3.2.1 产生原因

1. JVM 分配栈空间不足。

2. 调用方法过多，递归深度过深，导致栈帧过多，进而引发栈溢出。

3. 方法中本地变量等过多，导致单个栈帧过大，进而引发栈溢出。

`-Xss` 每个线程分配栈空间大小，`-Xoss` 每个线程分配本地方法栈大小。对于 Sun HotSpot 虚拟机，虚拟机栈和本地方法栈统一管理，该配置参数无效。

### 3.2.2 验证代码

```java
/**
 * VM Args：-Xss128k
 * 
 * @author moonspirit
 * @version 1.0
 */
public class StackSOF {

	private int stackLength = 1;

	public void stackLeak() {
		stackLength++;
		stackLeak();
	}

	public static void main(String[] args) throws Throwable {
		StackSOF oom = new StackSOF();
		try {
			oom.stackLeak();
		} catch (Throwable e) {
			System.out.println("stack length:" + oom.stackLength);
			throw e;
		}
	}
}
```

## 3.3 虚拟机栈和本地方法栈溢出（多线程）

多线程下，虚拟机栈异常抛出 OutOfMemoryError。

### 3.3.1 产生原因

`-Xss` 指定每个线程分配栈空间大小，在多线程程序中，该参数设置过大易出现栈内存溢出异常。

总内存空间 = 最大堆容量（Xmx） + 栈总量（n*Xss） + 最大方法区容量（MaxPermSize） + 其他（程序计数器等）

每个进程分配的栈容量越大，可以建立的线程数量就越少，越容易出现内存溢出。

### 3.3.2 验证代码

```java
/**
 * VM Args：-Xss2M
 * 
 * @author moonspirit
 * @version 1.0
 */
public class StackOOM {

	private void dontStop() {
		while (true) {
		}
	}

	public void stackLeakByThread() {
		while (true) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					dontStop();
				}
			});
			thread.start();
		}
	}

	public static void main(String[] args) throws Throwable {
		StackOOM oom = new StackOOM();
		oom.stackLeakByThread();
	}
}
```

## 3.4 方法区溢出

### 3.4.1 产生原因

#### 1. 运行时常量池溢出

JDK 1.7 以前，JVM 采用永久代实现方法区，应用 GC 分代垃圾收集机制。不断将新常量添加到方法区，会导致方法区溢出。

JDK 1.7 开始，运行时常量池已从永久代移除，该方法不再可能导致方法区溢出。其中，符号引用转移到 native heap，字面量转移到 java heap，类的静态变量转移到 java heap。运行时常量池存储的不再是对象，而是对象引用，真正的对象存储在堆中，通过 `-Xms` 和 `-Xmx` 限制堆空间可以产生异常，注意此时是 Java 堆溢出而不是方法区溢出。

JDK 1.8 中，永生代被彻底移除，永久代配置参数 `-XX:PermSize` 和 `-XX：MaxPermSize` 同时被移除，改用 `-XX:MetaspaceSize` 和 `-XX:MaxMetaspaceSize` 参数设置类的元数据的内存大小。

`-XX:PermSize=10M` 永生代初始分配内存大小，`-XX:MaxPermSize=10M` 永生代最大分配内存大小。

#### 2. 类信息溢出

类增强技术（CGLib、Spring）、JVM 上的动态语言（Groovy）、JSP 文件（JSP 首次运行时被编译成类）、基于OSGi的应用（同一个类文件被不同加载器加载视为不同的类）等会动态生成大量类，导致方法区溢出。

### 3.4.2 验证代码

```java
import java.util.ArrayList;
import java.util.List;

/**
 * VM Args：-Xms10m -Xmx10m
 * 
 * @since JDK1.6
 * @author moonspirit
 * @version 1.0
 */
public class RuntimeConstantPoolOOM {

	public static void main(String[] args) {
		List<String> list = new ArrayList<String>();
		int i = 0;
		while (true) {
			list.add(String.valueOf(i++).intern());
		}
	}
}
```

```java
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * VM Args：-XX:MetaspaceSize=10M -XX:MaxMetaspaceSize=10M
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
				@Override
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
```

## 3.5 直接内存溢出

### 3.5.1 产生原因

1. 程序中直接或间接使用 NIO。

2. 直接内存溢出不会再 Heap Dump 文件中观察到，溢出后 Dump 文件很小。

3. `-XX:MaxDirectMemorySize=10M` 分配直接内存大小，如不指定，默认与 `-Xmx` 配置空间相同。

### 3.5.2 验证代码

```java
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * VM Args：-Xmx20M -XX:MaxDirectMemorySize=10M
 * 
 * @author moonspirit
 * @version 1.0
 */
public class DirectMemoryOOM {

	private static final int _1MB = 1024 * 1024;

	public static void main(String[] args) throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredFields()[0];
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		while (true) {
			unsafe.allocateMemory(_1MB);
		}
	}
}
```
