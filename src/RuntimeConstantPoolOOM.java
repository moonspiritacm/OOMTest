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
