package crabzilla.stack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Provides dynamic dispatch on all method parameters, also
 * known as multi-methods. This is useful, for instance, when
 * implementing event-driven code or structuring a program
 * explicitly as a state-machine.
 * It is likely that we'll have something like this
 * natively in future versions of Java supported by the new
 * <a href="http://blogs.sun.com/roller/page/gbracha?entry=invokedynamic">invokedynamic opcode</a>.
 * <p/>
 * The latest version of the single implementation file
 * <a href="http://gsd.di.uminho.pt/members/jop/mm4j/MultiMethod.java">MultiMethod.java</a>
 * and of this document are available
 * at the <a href="http://gsd.di.uminho.pt/members/jop/mm4j/">mm4j homepage</a>.
 * Check the <a href="#changes">change log</a> for the latest news.
 * <p/>
 * This implementation builds on reflection capabilities in the Java language.
 * First, an exact match is attempted. If it does not exist, super-classes
 * of parameter values are also tested, starting from right to left. Currently,
 * interfaces are not considered, only super-classes. Matched methods are
 * cached and reused thus avoiding that the recursive matching
 * procedure is called repeatedly. This results in a fairly efficient implementation
 * which on the average adds only a single map lookup to the cost of normal
 * method invocation.
 * <p/>
 * Super-classes of right-most parameters are tested first, thus naturally
 * accommodating the native dynamic dispatch on this as the left-most
 * parameter. To better understand searching order, consider the
 * following sample program:
 * <pre>
 * import java.lang.reflect.*;
 * import mm4j.*;
 *
 * public class Test {
 *   public void m(String a, Integer b) {
 *     System.out.println("m(String,Integer) with "+
 *       a.getClass().getName()+" "+b.getClass().getName());
 *   }
 *   public void m(Object a, Integer b) {
 *     System.out.println("m(Object,Integer) with "+
 *       a.getClass().getName()+" "+b.getClass().getName());
 *   }
 *   public void m(String a, Object b) {
 *     System.out.println("m(String,Object) with "+
 *       a.getClass().getName()+" "+b.getClass().getName());
 *   }
 *   public void m(Object a, Object b) {
 *     System.out.println("m(Object,Object) with "+
 *       a.getClass().getName()+" "+b.getClass().getName());
 *   }
 *
 *   public static void main(String[] args) {
 *     try {
 *       Test t=new Test();
 *       MultiMethod mm=MultiMethod.getMultiMethod(t.getClass(), "m");
 *
 *       mm.invoke(t, "a", 2);
 *       mm.invoke(t, "a", "b");
 *       mm.invoke(t, 1, "b");
 *       mm.invoke(t, 1,2);
 *
 *       Method m=mm.resolve(String.class, Object.class);
 *       m.invoke(t, "a", 1);
 *       m=mm.resolve(String.class, String.class);
 *       m.invoke(t, "a", 1);
 *     } catch(Exception e) {e.printStackTrace();}
 *   }
 * };</pre>
 *
 * Notice that it uses the auto-boxing in Java5 for integers.
 * The sample program should produce the following output:
 * <pre>
 * m(String,Integer) with java.lang.String java.lang.Integer
 * m(String,Object) with java.lang.String java.lang.String
 * m(Object,Object) with java.lang.Integer java.lang.String
 * m(Object,Object) with java.lang.Integer java.lang.Integer
 * m(String,Object) with java.lang.String java.lang.Integer
 * m(String,Object) with java.lang.String java.lang.Integer</pre>
 *
 * Contrast the result of the second invocation, which
 * has (String, String) parameters and matches (String, Object), with the
 * result of the fourth invocation, which has (Integer, Integer) parameters
 * but matches (Object, Object) and not (Object, Integer).
 *
 * A particularly interesting idiom is achieved by using a Java5
 * variable parameter list to wrap dynamic invocation and exception handling as
 * follows:
 * <pre>
 * import java.lang.reflect.*;
 * import java.io.*;
 * import mm4j.*;
 *
 * interface Idiom {
 *   public void m(Object... args) throws NoSuchMethodException, IOException;
 * }
 *
 * public class IdiomImpl implements Idiom {
 *   // Alternatives
 *   public void m(Integer a) {  }
 *   public void m(String a, Object b) throws IOException {  }
 *   public void m(Integer a, String b, Object c) {  }
 *
 *   // Wrapper
 *   public void m(Object... args) throws NoSuchMethodException, IOException {
 *     try {
 *       mm.invoke(this, args);
 *     } catch(InvocationTargetException e) {
 *       Throwable target=e.getTargetException();
 *       if (target instanceof IOException)
 *         throw (IOException)target;
 *       else if (target instanceof RuntimeException)
 *         throw (RuntimeException)target;
 *
 *       target.printStackTrace();
 *     } catch(IllegalAccessException e) {
 *       e.printStackTrace();
 *     }
 *   }
 *   private static MultiMethod mm=MultiMethod.getMultiMethod(IdiomImpl.class, "m");
 *
 *   // Sample usage
 *   public static void main(String[] args) {
 *     try {
 *       Idiom t=new IdiomImpl();
 *
 *       t.m(3);
 *       t.m("a", "b");
 *       t.m(1, "b", 3f);
 *     } catch(Exception e) {e.printStackTrace();}
 *   }
 * };</pre>
 * Most of the effort is related to unwrapping InvocationTargetExceptions
 * to each of the possibilities. The resulting client code, in main, is however
 * very user friendly.
 * <hr>
 * <pre>
 *  mm4j.MultiMethod - Simple multi-methods for Java
 *  Copyright (C) 2005  Jose Orlando Pereira &lt;jop@di.uminho.pt&gt;
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *
 *   - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Minho nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</pre>
 * <hr>
 * <a name="changes"></a>Changes:
 * <ul>
 * <li>20051125 - Bug fix for null pointer exceptions in weak hash maps.</li>
 * <li>20051115 - Initial public release.</li>
 * </ul>
 */
@SuppressWarnings("rawtypes") public class MultiMethod {
  private static Map<String, MultiMethod> classCache = new HashMap<String, MultiMethod>();
  private Class claz;
  private String baseName;
  private Map<MultiMethod.Signature, Method> methodCache = new WeakHashMap<MultiMethod.Signature, Method>();

  private MultiMethod(Class claz, String methname) {
    this.claz = claz;
    this.baseName = methname;
  }

  /**
   * Lookup a dynamic method in a specific class. This method can
   * be used for invocations on instances of any derived class.
   * No validation is performed at this time and therefore there is
   * no guarantee that invocations will succeed, not even that there
   * is a method with the same name.
   *
   * @param claz a class, or superclass, of the target objects
   * @param name the name of the target method
   * @return a dynamic method object
   */
  public static MultiMethod getMultiMethod(Class claz, String name) {
    String mangled = claz.getName() + "+" + name;
    MultiMethod dynmeth = (MultiMethod) classCache.get(mangled);
    if (dynmeth == null) {
      dynmeth = new MultiMethod(claz, name);
      classCache.put(mangled, dynmeth);
    }
    return dynmeth;
  }

  private static final Class[] getTypes(Object[] args) {
    Class[] types = new Class[args.length];
    for (int i = 0; i < args.length; i++)
      types[i] = args[i].getClass();
    return types;
  }

  private static final Class[] copyTypes(Class[] args) {
    Class[] types = new Class[args.length];
    System.arraycopy(args, 0, types, 0, args.length);
    return types;
  }

  /**
   * Invokes the method represented by this MultiMethod object that
   * better fits the specified parameters on the specified object. The
   * same effect can be achieved by first resolving the method and
   * then using invoke. Note that this method cannot be used to invoke
   * target methods with primitive or null arguments.
   *
   * @param obj  the target object instance
   * @param args the argument values
   * @return the return value of the invoked method
   * @throws NoSuchMethodException                       no matching method found
   * @throws IllegalAccessException                      matching method exists but cannot be accessed
   * @throws java.lang.reflect.InvocationTargetException nested exception by target
   */
  public final Object invoke(Object obj, Object... args)
          throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    return resolveMethod(getTypes(args)).invoke(obj, args);
  }

  /**
   * Resolves the method represented by this MultiMethod object that
   * better fits the specified parameters types. The result can be
   * used repeatedly to avoid the overhead of matching. This method is
   * also able to resolve methods declared with primitive
   * arguments. The resulting method can also be invoked with
   * null arguments.
   *
   * @param types the classes of the argument of the method to be searched
   * @return a specific Method object
   * @throws NoSuchMethodException no matching method found
   */
  public final Method resolve(Class... types) throws NoSuchMethodException {
    return resolveMethod(copyTypes(types));
  }

  /**
   * Returns the Class object representing the class or interface
   * that declares the method family represented by this MultiMethod
   * object.
   *
   * @return the declaring class
   */
  public Class getDeclaringClass() {
    return claz;
  }

  /**
   * Returns the name of the target methods as a String.
   *
   * @return the name of the method
   */
  public String getName() {
    return baseName;
  }

  ;

  public String toString() {
    return claz.getName() + "." + baseName + "(...)";
  }

  @SuppressWarnings("unchecked")
  private Method search(Class[] types, int base) {
    if (base < 0)
      return null;
    Class argclaz = types[base];
    Method method = null;
    while (method == null) {
      try {
        method = claz.getMethod(baseName, types);
      } catch (NoSuchMethodException e) {
        // see below
      }
      if (method != null)
        break;
      types[base] = types[base].getSuperclass();
      if (types[base] == null)
        break;
      method = search(types, base - 1);
    }
    types[base] = argclaz;
    return method;
  }

  private final Method resolveMethod(Class... types) throws NoSuchMethodException {
    MultiMethod.Signature sign = new MultiMethod.Signature(types);
    Method method = (Method) methodCache.get(sign);
    if (method == null) {
      method = search(types, types.length - 1);

      // We also cache methods not found.
      methodCache.put(sign, method);
    }
    if (method == null)
      throw new NoSuchMethodException("no match found for " + baseName + sign);
    return method;
  }

  private static final class Signature {
    private Class[] types;

    public Signature(Class[] types) {
      this.types = types;
    }

    public boolean equals(Object other) {
      if (other == null)
        return false;
      return Arrays.equals(types, ((MultiMethod.Signature) other).types);
    }

    public int hashCode() {
      return Arrays.hashCode(types);
    }

    public String toString() {
      String name = "(";
      for (int i = 0; i < types.length; i++) {
        if (i != 0)
          name += ", ";
        name += types[i].getName();
      }
      return name + ")";
    }
  }
}
