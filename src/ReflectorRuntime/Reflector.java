package ReflectorRuntime;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * @author Mehmet Âkif SOLAK
 * Çalışma zamânında sınıf örneği (nesne) oluşturma,
 * dizi oluşturma gibi ihtiyaçların karşılanmasına yönelik
 * yardımcı sınıf
 * @version 1.0.1
 */
public class Reflector{
    private static Reflector serv;// service
    private HashMap<Class, Class> mapOfPrimitiveToWrapper;

    /**
     * 'getter' ve/veyâ 'setter' yöntemlerinin sınıf içerisinde
     * hangi isimle aranacağının bilinmesi için gerekli olan
     * kodlama biçimidir.
     * Misal, {@code "number"} özelliğine doğrudan zerk yapılamazsa,
     * {@code "CAMEL_CASE"} seçilirse, {@code "setNumber"}
     * {@code "SNAKE_CASE"} seçilirse, {@code "set_number"}
     * yöntemi kullanılarak ilgili değerin zerk edilmesi denenir.
     */
    public enum CODING_STYLE{
        CAMEL_CASE,
        SNAKE_CASE
    }
    /**
     * Yöntem (@code "Method") aranması işlemi yapılırken
     * aranan yöntemin tipinin belirtilmesini kolaylaştırmak
     * için kullanılan bir {@code 'enum'} sınıfıdır
     */
    public enum METHOD_TYPES{
        GET,
        SET
    }
    private Reflector(){}

// İŞLEM YÖNTEMLERİ:
    /**
     * 
     * @param <T> İstenen nesnenin tipi
     * @param classOfDataArray Dizisi istenen nesnenin sınıfı
     * @param length İstenen dizi uzunluğu
     * @return İstenen tipte dizi veyâ {@code null} döndürülür.
     */
    public <T> T[] produceNewArray(Class<T[]> classOfDataArray, int length){// Verilen dizi tipinde, verilen uzunlukta yeni bir değişken oluştur
        T[] value = null;
        try{
            value = classOfDataArray.cast(Array.newInstance(classOfDataArray.getComponentType(), length));
            return value;
        }
        catch(ClassCastException | NegativeArraySizeException | IllegalArgumentException | NullPointerException exc){
            System.err.println("İstenen sınıfta bir dizi oluşturulamadı : " + exc.toString());
            return null;
        }
    }
    /**
     * 
     * @param <T> İstenen nesnenin tipi
     * @param classOfDataArray Dizisi istenen nesnenin sınıfı
     * @param length İstenen dizi uzunluğu
     * @return İstenen tipte dizi (nesne olarak) {@code null} döndürülür.
     */
    public <T> T produceNewArrayReturnAsObject(Class<T> classOfDataArray, int length){// Yukarıdaki fonksiyonun aynısı; fakat temel veri tiplerinin dizisi için de çalışır, bi iznillâh..
        try{
            T value = classOfDataArray.cast(Array.newInstance(classOfDataArray.getComponentType(), length));
            return value;
        }
        catch(ClassCastException | NegativeArraySizeException | IllegalArgumentException | NullPointerException exc){
            System.err.println("İstenen sınıfta bir dizi oluşturulamadı : " + exc.toString());
            return null;
        }
    }
    /**
     * 
     * @param <T> İstenen nesnenin tipi
     * @param classOfDataArray Dizisi istenen nesnenin sınıfı
     * @param length İstenen dizi uzunluğu
     * @param data Dizi elemanlarını belirten liste
     * @return Verilen değerler diziye atanmış olarak istenen tipte dizi
     * nesne formunda döndürülür veyâ {@code null} döndürülür.
     */
    public <T> T produceNewArrayInjectDataReturnAsObject(Class<T> classOfDataArray, int length, List data){
        try{
            Object value = Array.newInstance(classOfDataArray.getComponentType(), length);
            for(int sayac = 0; sayac < data.size(); sayac++){
                Array.set(value, sayac, data.get(sayac));
            }
            ".".split("\\.");
            return classOfDataArray.cast(value);
        }
        catch(NegativeArraySizeException | IllegalArgumentException | NullPointerException | ArrayIndexOutOfBoundsException exc){
            System.err.println("Verilen verilerle istenen sınıfta bir dizi oluşturulamadı : " + exc.toString());
            return null;
        }
    }
    /**
     * 
     * @param <T> Örneği istenen sınıf, tip olarak
     * @param cls Nesnesi üretilmek istenen sınıf
     * @return Verilen sınıfın ilklendirilmiş bir örneği
     */
    public <T> T produceNewInstance(Class<T> cls){// Arayüz oluşturulurken hatâ vermemesi için kod eklenecek ; generic oluşturmak için kod eklenecek bi iznillâh
        // Temel veri tipleri için tespit, sargılayıcı sınıftan yeni nesne üretme ve temel veri tipine dönüştürme uygulanıyor:
        T obj = null;
        boolean unwrapOnFinal = false;
        boolean isPri = false;
        Class target;
        if(cls.isPrimitive()){// Temel veri tipiyse, işâretlemeleri yap
            isPri = true;
            unwrapOnFinal = true;
        }
//        if(cls.isArray()){// Dizi olup, olmadığı sorgulanmalı
//            //.;.
//        }
        try{
            if(cls.equals(List.class))
                target = ArrayList.class;
            else if(cls.equals(Map.class))
                target = HashMap.class;
            else if(isPri)
                target = getWrapperClassFromPrimitiveClass(cls);
            else
                target = cls;
            // Yeni sınıf örneği (nesne) oluşturmak için yapıcı yöntemlere bak:
            Constructor noParamCs = findConstructorForNoParameter(target);// İlk olarak parametresiz yapıcı yöntem ara:
            if(target.equals(Number.class))// Bu bir soyut sınıf olduğundan dolayı bunun kendi tipinde bir örneği oluşturulamaz; bu sebeple bunun için özel kod yazıyoruz
                obj = (T) ((Integer) 0);
            else if(noParamCs != null){
                obj = (T) noParamCs.newInstance(null);
            }
            else{// Parametresiz yapıcı yöntem yoksa; temel veri tipinin sarmalanmış hâli ise oluşturmaya çalış
                String parameterOfConstructor = getParameterForConstructorOfWrapperBasicClass(target);
                if(parameterOfConstructor != null){
                    obj = (T) target.getConstructor(String.class).newInstance(parameterOfConstructor);
                }
                else if(target.equals(Character.class)){
                    obj = (T) target.getConstructor(char.class).newInstance(' ');
                }
                else// İlgili sınıfın yeni bir örneği oluşturulamadı! : Burası belki geliştirilebilir...
                    return null;
            }
        }
        catch(InstantiationException exc){
            System.err.println("Hatâ - InstantiationException (produceNewInstance) : " + exc.toString());
        }
        catch(IllegalAccessException exc){
            System.err.println("Hatâ - IllegalAccessException (produceNewInstance) : " + exc.toString());
        }
        catch(InvocationTargetException exc){
            System.err.println("Hatâ - InvocationTargetException (produceNewInstance) : " + exc.toString());
        }
        catch(NoSuchMethodException exc){
            System.err.println("Hatâ - NoSuchMethodException (produceNewInstance) : " + exc.toString());
        }
        catch(IllegalArgumentException exc){
            System.err.println("Hatâ - IllegalArgumentException (produceNewInstance) : " + exc.toString());
        }
        catch(SecurityException exc){
            System.err.println("Yapıcı yöntem alınırken hatâ : " + exc.toString());
        }
        catch(ClassCastException exc){
            System.err.println("Veri tipi dönüşümü hâtası : " + exc.toString());
        }
        return obj;
    }
    /**
     * 
     * @param wrapperClass Sarmalayıcı sınıf
     * @return Sarmalayıcı sınıfın karşılığı olan temel {@code "primitive"}
     * sınıf veyâ null döndürülür.
     */
    public Class getPrimitiveClassFromWrapper(Class wrapperClass){
        Class value = null;
        for(Class cls : getMapOfPrimitiveToWrapper().keySet()){
            if(getMapOfPrimitiveToWrapper().get(cls).equals(wrapperClass)){
                value = cls;
                break;
            }
        }
        return value;
    }
    /**
     * 
     * @param primitiveClass Sarmalayıcısı istenen temel veri tipinin sınıfı
     * @return Verilen temel veri tipi sınıfının sarmalayıcısı veyâ {@code null}
     */
    public Class getWrapperClassFromPrimitiveClass(Class primitiveClass){
        return getMapOfPrimitiveToWrapper().get(primitiveClass);
    }
    /**
     * 
     * @param cls Sınıf
     * @return Verilen sınıfın parametresiz yapıcı yöntemi veyâ {@code null}
     */
    public Constructor findConstructorForNoParameter(Class cls){
        try{
            for(Constructor cs : cls.getConstructors()){
                if(cs.getParameterCount() == 0)
                    return cs;
            }
        }
        catch(SecurityException exc){
            System.err.println("Sınıf yapıcı yöntemleri aranırken hatâ alındı : " + exc.toString());
        }
        return null;
    }
    /**
     * 
     * @param <T> Dönüşüm yapılması istenen sınıf tipi
     * @param data Metîn hâlinde bulunan veri
     * @param target Verilen verinin dönüştürülmesi istenen veri tipi
     * @return Verilen verinin hedef veri tipindeki nesne hâli veyâ {@code null}
     */
    public <T> T getCastedObjectFromString(String data, Class<T> target){// Verilen metîndeki veriyi verilen tipte bir nesneye dönüştür
        if(data == null || target == null)
            return null;
        if(data.isEmpty())
            return null;
        if(target == String.class)
            return ((T) new String(data));
        try{
        Object casted = null;
            if(target == Integer.class || target == int.class)// Tamsayı ise;
                casted = Integer.valueOf(data);
            else if(target == Double.class || target == double.class)
                casted = Double.valueOf(data);
            else if(target == Float.class || target == float.class)
                casted = Float.valueOf(data);///e
            else if(target == Byte.class || target == byte.class)
                casted = Byte.valueOf(data);
            else if(target == Long.class || target == long.class)
                casted = Long.valueOf(data);
            else if(target == Short.class || target == short.class)
                casted = Short.valueOf(data);
            else if(target == Boolean.class || target == boolean.class)
                casted = Boolean.valueOf(data);
            else if(target == Character.class || target == char.class)
                casted = data.charAt(0);// İlk karakter alınıyor
            return (T) casted;
        }
        catch(ClassCastException | NumberFormatException exc){
            System.err.println("İstenen veri tipine dönüştürülemedi : " + exc.toString());
            return null;
        }
    }
    /**
     * Verilen değerleri verilen sınıftaki nesneye zerk ederek nesne üretmeye çalışır.
     * Bu sürümde parametresiz yapıcı yöntemi bulunmayan sınıfların örneği üretilemiyor.
     * {@code enum} değerler için "getter" erişim yöntemi aranmıyor; yanî enum değerin
     * gizli olmaması lazım.
     * @param <T> Sınıf örneği istenen sınıf
     * @param targetClass Örneği istenen sınıf
     * @param data Sınıfın nesnesi oluşturulurken zerk edilmesi istenen özellik değerleri
     * @param codeStyleNeededOnSearchMethod Sınıfın 'setter' yöntemleri aranması durumunda,
     * 'getter' yöntem isminin hangi kodlama standardına göre aranacağı bilgisi
     * @param tryToForceCastForParameterType Sınıfın bir özelliğinin veri tipi, verilen
     * sınıf 'data' haritasındaki ilgili elemanın veri tipiyle uyuşmazsa
     * hedef veri tipine dönüştürmeye ('casting') çalışılmasını ifâde eden bayrak
     * @return Sınıf örneği (Özellik (field) değerleri zerk edilmiş nesne)
     */
    public <T> T pruduceNewInjectedObject(Class<T> targetClass, Map<String, Object> data, CODING_STYLE codeStyleNeededOnSearchMethod, boolean tryToForceCastForParameterType/*, boolean isIncludeNoParameterConstructor, List<Object> parameterForConstructor*/){
        try{// Hedef veri tipinin uygunluğunu kontrol et
            if(!checkTargetClassForInjection(targetClass))
                return null;
        }
        catch(IllegalArgumentException exc){
            System.err.println(exc.toString());
            return null;
        }
//        if(isIncludeNoParameterConstructor){} : Eklenecek inşâAllâh
//          else
        T obj = produceNewInstance(targetClass);
        if(obj == null || data == null)// Hedef veri tipinin örneği oluşturulamadıysa veyâ verilen özellik haritası = null
            return null;
        if(data.isEmpty())// Verilen özellik haritasında bir özellik yoksa..
            return obj;
        try{
            Field[] fields = targetClass.getDeclaredFields();
            for(Field fl : fields){
                Object value = data.get(fl.getName());
                if(value == null){
                    if(fl.getType().isPrimitive())
                        continue;// Temel veri tipindeki bir alana null zerk edilemez, bu alanı atla
                }
                if(fl.getType().isEnum()){
                    Class founded = getEnumClass(fl.getType().getName());
                    if(founded == null){// İlgili alanın tipi olan 'enum' sınıfı bulunamadı
                        System.err.println("İlgili alanın tipi olan 'enum' sınıfı bulunamadı!");
                        continue;
                    }
                    if(value != null){// Metîn olarak verilen 'enum' değerlerin enum değere dönüştürülmesi işlemi:
                        boolean enumValueFounded = false;
                        if(value instanceof String){// 'enum' değer veri haritasında metîn olarak saklanıyorsa;
                            for(Object enumValue : founded.getEnumConstants()){
                                if(enumValue.toString().equals(value.toString())){
                                    value = enumValue;
                                    enumValueFounded = true;
                                }
                            }
                            if(!enumValueFounded){// Değer metîn ise ve karşılığı olan enum değer bulunamadıysa;
                                // enum değerin 'getter' yöntemi aranması uygunsa, buraya ilâve edilebilir
                                value = null;// Hedef özelliğe 'null' değerini zerk et.
                            }
                        }
                    }
               }
                if(tryToForceCastForParameterType && value != null){
                    if(value.getClass() != fl.getType()){// Hedef özelliğin veri tipi ile verilen değerin veri tipi aynı değilse!
                        if(!value.getClass().isPrimitive() && !fl.getType().isPrimitive()){
                            // Hedef özelliğin veri tipine çevirmeye çalış:
                            try{
        //                        System.err.println("Şu alanın veri tipi verilen değerin veri tipiyle uyuşmuyor : " + fl.getName());
                                Object castedValue = fl.getType().cast(value);
                                value = castedValue;
                            }
                            catch(ClassCastException flCastException){
                                System.err.println("Şu alanın veri tipi verilen değerin veri tipiyle uyuşmuyor : " + fl.getName());
                                //Buraya continue; yazmıyorum; çünkü belki 'setter' yöntemi bu veri tipinden değişkeni parametre olarak kabûl ediyordur
                            }
                        }
                    }
                }
                try{
                    fl.set(obj, value);// Basit zerk : Erişim izni olmayan alanlar için çalışmaz!
                }
                catch(IllegalAccessException | IllegalArgumentException excOnBasicInjection){// Hedef özelliğe doğrudan erişim izni yok veyâ parametrenin veri tipi uyuşmuyor, diğer yöntemleri dene
//                    System.err.println("exc : " + exc.toString());
                    // Veriyi hedef özelliğe zerk edebilmek için 'setter' yöntemi ara:
                    String methodName = getMethodNameDependsCodeStyle(fl.getName(), codeStyleNeededOnSearchMethod, METHOD_TYPES.SET);
                    ArrayList<Method> getters = new ArrayList<Method>();
                    try{
                        for(Method m : targetClass.getDeclaredMethods()){
                            if(m.getName().equals(methodName))
                                getters.add(m);
                        }
                    }
                    catch(SecurityException excOnTakingMethods){
                        System.err.println("Sınıfın yöntem isimleri alınamadı : " + excOnTakingMethods.toString());
                        continue;
                    }
                    if(getters.isEmpty())
                        continue;
                    for(int index = 0; index < getters.size(); index++){
                        try{
                            getters.get(index).invoke(obj, value);
                            break;// Başka 'setter' yöntemi varsa onları uygulamaya çalışma!
                        }
                        catch(SecurityException excOnInvokingSetter){
                            System.err.println("Hedef özelliğin 'setter' yöntemi güvenlik sebebiyle çalıştırılamadı! : " + excOnInvokingSetter);
                        }
                        catch(ExceptionInInitializerError | InvocationTargetException exc2OnInvokingSetter){
                            System.err.println("Hedef özelliğin 'setter' yöntemi çalıştırılamadı : " + exc2OnInvokingSetter.toString());
                        }
                        catch(IllegalArgumentException exc3OnInvokingSetter){
                            System.err.println("Hedef özelliğin 'setter' yöntemi için geçersiz parametre verildi : " + exc3OnInvokingSetter.toString());
                        }
                        catch(IllegalAccessException exc4OnInvokingSetter){
                            System.err.println("Hedef özelliğin 'setter' yöntemi çalıştırılırken erişim hatâsı alındı : " + exc4OnInvokingSetter.toString());
                        }
                    }
                }
            }
        }
        catch(SecurityException exc){
            System.err.println("exc : " + exc.toString());
        }
        return obj;
    }
    // ARKAPLAN İŞLEM YÖNTEMLERİ:
    private String getParameterForConstructorOfWrapperBasicClass(Class cls){
        String param = null;
        if(cls.equals(Integer.class))
            param = "0";
        else if(cls.equals(Double.class))
            param = "0.0";
        else if(cls.equals(Long.class))
            param = "0";
        else if(cls.equals(Short.class))
            param = "0";
        else if(cls.equals(Number.class))
            param = "0";
        else if(cls.equals(Boolean.class))
            param = "true";
        else if(cls.equals(Byte.class))
            param = "0";
        return param;
    }
    /**
     * Sınıf örneği oluşturulması için verilen sınıfın uygun
     * olup, olmadığıyla ilgili temel kontrol yapar.
     * @param cls Zerk edilmek istenen sınıf
     * @return Sınıfın zerk edilmeye uygun olup, olmadığı bilgisi
     * döndürülür. Sınıfın null, arayüz, dizi veyâ temel veri tipi olması
     * durumunda {@code false}, diğer durumda {@code true} döndürülür
     */
    private boolean checkTargetClassForInjection(Class cls){
        try{
            if(cls == null)
                return false;
            if(cls.isPrimitive())
                throw new IllegalArgumentException("Hedef veri tipi temel bir veri tipi olamaz");
            if(cls.isArray())
                throw new IllegalArgumentException("Hedef veri tipi bir dizi olamaz");
            if(cls.isInterface())
                throw new IllegalArgumentException("Hedef veri tipi bir arayüz olamaz");
            return true;
        }
        catch(IllegalArgumentException exc){
            return false;
        }
    }
    private String convertFirstLetterToUpper(String s){
        String firstLetter = s.substring(0, 1);
        firstLetter = firstLetter.toUpperCase(Locale.ENGLISH);
        return (firstLetter + s.substring(1));
    }
    private String getMethodNameDependsCodeStyle(String nameOfAttribute, CODING_STYLE codingStyle, METHOD_TYPES targetMethodType){
        if(codingStyle == CODING_STYLE.CAMEL_CASE){
            String preText = (targetMethodType == METHOD_TYPES.GET ? "get" : "set");
            return preText + convertFirstLetterToUpper(nameOfAttribute);
        }
        return null;
    }
    private Class<?> getEnumClass(String className){
        return getLoadedClass(className, true);
    }
    private Class<?> getLoadedClass(String className, boolean isEnumClass){
        try{
            return ClassLoader.getSystemClassLoader().loadClass(className);
        }
        catch(ClassNotFoundException exc){
            System.err.println("Sınıf bulunamadı : " + exc.toString());
            return null;
        }
    }

// ERİŞİM YÖNTEMLERİ:
    //ANA ERİŞİM YÖNTEMİ
    /**
     * 
     * @return {@code "Reflector"} servisi
     */
    public static Reflector getService(){
        if(serv == null)
            serv = new Reflector();
        return serv;
    }
    // GİZLİ ERİŞİM YÖNTEMLERİ:
    private HashMap<Class, Class> getMapOfPrimitiveToWrapper(){
        if(mapOfPrimitiveToWrapper == null){
            mapOfPrimitiveToWrapper = new HashMap<Class, Class>();
            mapOfPrimitiveToWrapper.put(int.class, Integer.class);
            mapOfPrimitiveToWrapper.put(double.class, Double.class);
            mapOfPrimitiveToWrapper.put(float.class, Float.class);
            mapOfPrimitiveToWrapper.put(boolean.class, Boolean.class);
            mapOfPrimitiveToWrapper.put(short.class, Short.class);
            mapOfPrimitiveToWrapper.put(long.class, Long.class);
            mapOfPrimitiveToWrapper.put(char.class, Character.class);
            mapOfPrimitiveToWrapper.put(byte.class, Byte.class);
        }
        return mapOfPrimitiveToWrapper;
    }
}