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
