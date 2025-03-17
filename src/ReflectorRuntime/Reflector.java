package ReflectorRuntime;

import java.io.File;
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
 * Çalışma zamânında verileri zerk ederk sınıf örneği (nesne) oluşturma,
 * dizi - liste oluşturma gibi ihtiyaçların karşılanmasına yönelik
 * yardımcı sınıf
 * @version 2.0.5
 */
public class Reflector{
    private static Reflector serv;// service
    private HashMap<Class, Class> mapOfPrimitiveToWrapper;
    private HashMap<Class, Method> numberMethods;
    private final List<String> basicDataTypes = getBasicDataTypes();

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
     * Verilen bilgilerle boyut bağımsız dizi oluşturun
     * @param <T> Hedef dizi sınıfını belirten tip
     * @param classOfDataArray Hedef sınıf, misal {@code int[][].class} gibi..
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @return İstenen verilerin zerk edildiği dizi örneği veyâ {@code null}
     */
    public <T> T produceNewArrayInjectDataReturnAsObject(Class<T> classOfDataArray, Object data){// Hedef diziye dönüştürme, derinlik bağımsız
        if(data == null)
           return null;
        boolean isDataAnArray = false;
        try{// Veri dizi veyâ liste biçiminde olmalıdır:
            if(!data.getClass().isArray()){
                List li = (List) data;
                if(li == null)
                    return null;
            }
            else
                isDataAnArray = true;
        }
        catch(ClassCastException exc){
            System.err.println("Yalnızca List ve dizi biçimindeki veriler kabûl ediliyor.");
            return null;
        }
        try{
            int dimension = (isDataAnArray ? getDimensionOfArray(classOfDataArray) : getDimensionOfList((List) data));
            boolean dimensionIsEqualOne = (dimension == 1);
            int len = (isDataAnArray ? Array.getLength(data) : ((List) data).size());
            Object value = Array.newInstance(classOfDataArray.getComponentType(), len);
            for(int sayac = 0; sayac < len; sayac++){
                Object element = (isDataAnArray ? Array.get(data, sayac) : ((List) data).get(sayac));
                if(element == null && dimensionIsEqualOne && classOfDataArray.getComponentType().isPrimitive())// Temel veri tipine doğrudan null değeri atanamaz
                    continue;
                if(dimensionIsEqualOne)
                    Array.set(value, sayac, element);
                else
                    Array.set(value, sayac,
                        produceNewArrayInjectDataReturnAsObject(classOfDataArray.getComponentType(),
                            element));
            }
            return (T) value;
        }
        catch(IllegalArgumentException | ClassCastException exc){
            System.err.println("exc : " + exc.toString());
        }
        return null;
    }
    /**
     * Verilen verilerle boyut bağımsız liste oluşturun
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @return İstenen verilerin zerk edildiği liste veyâ {@code null}
     */
    public List produceNewInjectedList(Object data){
        if(data == null)
           return null;
        boolean isDataAnArray = false;
        try{// Veri dizi veyâ liste biçiminde olmalıdır:
            if(!data.getClass().isArray()){
                List li = (List) data;
                if(li == null)
                    return null;
            }
            else
                isDataAnArray = true;
        }
        catch(ClassCastException exc){
            System.err.println("Yalnızca List ve dizi biçimindeki veriler kabûl ediliyor.");
            return null;
        }
        List value = new ArrayList();
        try{
            int len = (isDataAnArray ? Array.getLength(data) : ((List) data).size());
            if(len == 0)
                return value;
            int dimension = (isDataAnArray ? getDimensionOfArray(data.getClass()) : getDimensionOfList((List) data));
            boolean dimensionIsEqualOne = (dimension == 1);
            for(int sayac = 0; sayac < len; sayac++){
                Object element = (isDataAnArray ? Array.get(data, sayac) : ((List) data).get(sayac));
                if(dimensionIsEqualOne)
                    value.add(sayac, element);
                else
                    value.add(sayac, produceNewInjectedList(element));
            }
            return value;
        }
        catch(IllegalArgumentException exc){
            System.err.println("exc : " + exc.toString());
        }
        return null;
    }
    /**
     * Verilen tiplerinin birbirine otomatik olarak dönüşebildiği denetleniyor
     * Java otomatik sarmalama sebebiyle sarmalanan sınıf ile temel hâli eşleşir
     * @param cls1 Sınıf - 1
     * @param cls2 Sınıf - 2
     * @return Eğer otomatik dönüşüyorsa {@code true}, değilse {@code false} 
     */
    public boolean isPairingAutomatically(Class cls1, Class cls2){
        Class founded = getMapOfPrimitiveToWrapper().get(cls1);
        if(founded == null){
            founded = getMapOfPrimitiveToWrapper().get(cls2);
            if(founded == null)
                return false;
            else
                if(cls1.equals(founded))
                    return true;
        }
        else{
            if(cls2.equals(founded))
                return true;
        }
        return false;
    }
    /**
     * Sarmalayıcı sınıftan temel veri tipini elde edin
     * @param wrapperClass Sarmalayıcı sınıf
     * @return Sarmalayıcı sınıfın karşılığı olan temel {@code "primitive"}
     * sınıf veyâ null döndürülür
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
     * Temel veri tipinden sarmalayıcı sınıfı elde edin
     * @param primitiveClass Sarmalayıcısı istenen temel veri tipinin sınıfı
     * @return Verilen temel veri tipi sınıfının sarmalayıcısı veyâ {@code null}
     */
    public Class getWrapperClassFromPrimitiveClass(Class primitiveClass){
        return getMapOfPrimitiveToWrapper().get(primitiveClass);
    }
    /**
     * Verilen sınıfın parametresiz yapıcı yöntemini arar
     * @param cls Hedef ssınıf
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
     * Metîn biçiminde depolanan farklı veri tiplerindeki veriyi dönüştürür
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
     * Verilen veriyi hedef sınıftaki nesneye zerk ederek nesne üretmeye çalışır
     * Şu an parametresiz yapıcı yöntemi bulunmayan sınıfın örneği üretilemiyor
     * {@code enum} değerler için "getter" erişim yöntemi aranmıyor; yanî enum 
     * değerin gizli olmaması lazım.
     * @param <T> Sınıf örneği istenen sınıf
     * @param targetClass Örneği istenen sınıf
     * @param data Sınıfın örneğine zerk edilmesi istenen özellik değerleri
     * @param codeStyleNeededOnSearchMethod 'setter' yöntemine ihtiyaç duyulması
     * durumunda bu yöntemin hangi kodlama standardına göre aranacağı bilgisi
     * @return Verilen verilerin zerk edildiği sınıf örneği veyâ {@code null}
     */
    public <T> T pruduceNewInjectedObject(Class<T> targetClass, Map<String, ? extends Object> data, CODING_STYLE codeStyleNeededOnSearchMethod){
        return pruduceNewInjectedObject(targetClass, data, codeStyleNeededOnSearchMethod, true, false, null);
    }
    /**
     * 
     * @param <T> Verilen nesnenin tipi
     * @param targetObject Değerlerin zerk edilmesi istenen sınıf örneği (nesne)
     * @param data Sınıf örneğine zerk edilmesi istenen özellik değerleri
     * @param codeStyleNeededOnSearchMethod 'setter' yöntemine ihtiyaç duyulması
     * durumunda bu yöntemin hangi kodlama standardına göre aranacağı bilgisi
     * @return Verilerin zerk edildiği nesne veyâ başarısız ise {@code null}
     */
    public <T> T injectData(T targetObject, Map<String, ? extends Object> data, CODING_STYLE codeStyleNeededOnSearchMethod){
        if(targetObject == null)
            return null;
        return pruduceNewInjectedObject((Class<T>) targetObject.getClass(), data, codeStyleNeededOnSearchMethod, true, true, targetObject);
    }
    /**
     * Verilen dizi sınıfına bakarak dizi boyutunu döndürür
     * @param cls Dizi sınıfı, misal {@code int[][].class} gibi..
     * @return Dizi boyutu, sınıf dizi sınıfı değilse veyâ {@code null} ise 0
     */
    public int getDimensionOfArray(Class cls){
        if(cls == null)
            return 0;
        return (cls.getTypeName().split("\\[").length - 1);
    }
    /**
     * Verilen listenin elemanlarını tarayarak listenin derînliğini araştırır
     * Eğer dizi elemanı bir liste ise, derinlik bir arttırılır ve o da taranır
     * @param list Liste
     * @return Listenin derînliği, liste {@code null} ise {@code 0} döndürülür
     */
    public int getDimensionOfList(List list){
        if(list == null)
            return 0;
        return findDepthWhole(list, 1);
    }
    /**
     * Verilen sınıfın özelliklerini alır ve {@code Map} tipinde döndürür.
     * @param obj Özellik değerleri istenen nesne
     * @param codingStyleForGetMethods Özellikleri almak için gerekebilecek
     * 'getter' yöntemlerinin isminin belirlenmesi için kullanılan kodlama tipi
     * @return Verilen nesnenin özellikleri yâ dâ {@code null}
     */
    public Map<String, Object> getFieldValuesBasicly(Object obj, Reflector.CODING_STYLE codingStyleForGetMethods){
        if(obj == null)
            return null;
        Field[] fields = null;
        Map<String, Object> result = new HashMap<String, Object>();
        try{
            fields = obj.getClass().getDeclaredFields();
            for(Field fl : fields){
                Object value = null;
                try{
                    value = fl.get(obj);
                }
                catch(IllegalArgumentException | IllegalAccessException excOnTakingFieldValue){
                    System.err.println("excOnTakingFieldValue : " + excOnTakingFieldValue);
                    String getMethodName = getMethodNameDependsCodeStyle(fl.getName(), codingStyleForGetMethods, METHOD_TYPES.GET);
                    try{
                        Method m = obj.getClass().getMethod(getMethodName, null);
                        if(m != null){
                            value = m.invoke(obj, null);
                        }
                    }
                    catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException excOnInvokingGetMethod){
                        System.err.println("excOnTakingInvokingGetMethod : " + excOnTakingFieldValue.toString());
                        continue;
                    }
                }
                result.put(fl.getName(), value);
            }
        }
        catch(SecurityException exc){
            System.err.println("exc : " + exc.toString());
            return null;
        }
        return result;
    }
    /**
     * Verilen sınıfın verilen alanlarını {@code Field} listesi olarak döndürür
     * @param cls Alanları alınmak istenen hedef sınıf
     * @param fieldNames İstenen alanlar, {@code null} veyâ boş ise hepsi alınır
     * @return Hedef sınıfın ilgili alanlarının listesi veyâ {@code null}
     */
    public List<Field> getSpecifiedFields(Class cls, List<String> fieldNames){
        if(cls == null)
            return null;
        List<Field> result = new ArrayList<Field>();
        boolean takeAll = false;
        if(fieldNames == null)
            takeAll = true;
        else if(fieldNames.isEmpty())
            takeAll = true;
        if(takeAll){
            for(Field fl : cls.getDeclaredFields())
                result.add(fl);
        }
        else{
            for(String name : fieldNames){
                Field fl = null;
                try{
                    fl = cls.getDeclaredField(name);
                }
                catch(NoSuchFieldException | SecurityException exc){
                    System.err.println("İstenen alan alınamadı : " + exc.toString());
                }
                if(fl != null)
                    result.add(fl);
            }
        }
        return result;
    }
    public boolean isDataTypeBasic(String dataTypeName){
        for(String s : this.basicDataTypes){
            if(s.equals(dataTypeName))
                return true;
        }
        return false;
    }
    /**
     * Verilen listenin elemanlarını tarayarak listenin derînliğini araştırır
     * Eğer dizi elemanı bir liste ise, derinlik bir arttırılır ve o da taranır
     * @param list Liste
     * @param depth Derinlik, yöntemi çağırırken {@code 1} değeri verilmeli
     * @return Listenin derînliği, liste {@code null} ise {@code 0} döndürülür
     */
    public int findDepthWhole(List list, int depth){
        if(list == null)
            return 0;
        int len = list.size();
        if(len <= 0)
            return depth;
        int[] lengths = new int[len];
        for(int sayac = 0; sayac < len; sayac++){
            Object element = list.get(sayac);
            if(element == null)
                lengths[sayac] = depth;
            else if(list.get(sayac) instanceof List)
                lengths[sayac] = findDepthWhole((List) list.get(sayac), depth + 1);
            else
                lengths[sayac] = depth;
        }
        int max = lengths[0];
        for(int sayac = 1; sayac < len; sayac++){
            if(lengths[sayac] > lengths[sayac - 1])
                max = lengths[sayac];
        }
        return max;
    }
    /**
     * Liste veyâ dizi şeklindeki bir alana doğrudan zerk edilemeyen liste veyâ
     * dizi şeklindeki bir veriyi zerk edebilmek için gereken dönüşümü yapar
     * @param value Hedef alana zerk edilmek istenen veri, liste veyâ dizi
     * @param target Hedef alan
     * @return Hedef alan ile uyumlu biçimdeki veri veyâ {@code null}
     */
    public Object checkAndConvertListAndArray(Object value, Field target){
        // İki soruna çözüm aranıyor:
        // 1) value = List ve element = Array ise uyumluluk arayıp, atama yapma
        // 2) value = Array ve element = List ise, uyumluluk arayıp, atama yapma
        if(value == null || target == null)
            return null;
        boolean isValueAnArray = value.getClass().isArray();
        boolean isTargetAnArray = target.getType().isArray();
        boolean isValueAnList = false;
        boolean isTargetAnList = false;
        if(!isValueAnArray){
            if(value instanceof List)
                isValueAnList = true;
        }
        if(!isTargetAnArray){
            try{
                Class casted = target.getType().asSubclass(List.class);
                if(casted != null)
                    isTargetAnList = true;
            }
            catch(ClassCastException exc){
                System.err.println("exc : " + exc.toString());
                isTargetAnList = false;
            }
        }
        if((!isTargetAnArray && !isTargetAnList) || (!isValueAnArray && !isValueAnList))
            return null;// Taraflardan herhangi birisi hem liste, hem de dizi değilse "null" döndür
        if(isTargetAnArray){// Hedef bir dizi ise..
            int dimensionOfTarget = getDimensionOfArray(target.getType());// Hedef dizinin boyutu
            int dimensionOfValue = findDepthWhole((List) value, 1);// Veri dizisinin boyutu
            if(dimensionOfValue > dimensionOfTarget)// Hedef ile veri arasında dizi boyutu uyuşmazlığı var
                return null;
            return produceNewArrayInjectDataReturnAsObject(target.getType(), value);
        }
        else{// Hedef bir liste ise...
            return produceNewInjectedList(value);
        }
    }
    /**
     * İsmi verilen bir özelliğin ('attribute') 'getter' veyâ 'setter' yöntem
     * ismini döndürür
     * @param nameOfAttribute Özelliğin ismi
     * @param codingStyle İlgili yöntemin kodlanırken kullanılan kod biçimi
     * @param targetMethodType İsmi istenen yöntem tipi{@code GET} | {@code SET}
     * @return Hedef yöntemin ismi veyâ {@code null}
     */
    public String getMethodNameDependsCodeStyle(String nameOfAttribute, CODING_STYLE codingStyle, METHOD_TYPES targetMethodType){
        if(codingStyle == CODING_STYLE.CAMEL_CASE){
            String preText = (targetMethodType == METHOD_TYPES.GET ? "get" : "set");
            return preText + convertFirstLetterToUpper(nameOfAttribute);
        }
        else if(codingStyle == CODING_STYLE.SNAKE_CASE){
            String preText = (targetMethodType == METHOD_TYPES.GET ? "get" : "set");
            return preText + "_" + nameOfAttribute.toLowerCase(Locale.ENGLISH);
        }
        return null;
    }
    /**
     * Verilen nesnenin verilen alanlarını almak için kullanışlı bir yöntem
     * @param entity Sınıf örneği (nesne)
     * @param fields Değeri alınmak istenen alanlar
     * @param codingStyle Nesnenin 'getter' yönteminin kodlama biçimi
     * @return Verilen alanların değerleri veyâ boş bir {@code Map}
     * {@code null} parametre verilmesi durumunda {@code null} döndürülür
     */
    public Map<String, Object> getValueOfFields(Object entity, Field[] fields, CODING_STYLE codingStyle){
        if(entity == null || fields == null)
            return null;
        Map<String, Object> values = new HashMap<String, Object>();
        for(Field fl : fields){
            if(fl == null)
                continue;
            Object value = null;
            try{
                value = fl.get(entity);
            }
            catch(IllegalAccessException | IllegalArgumentException exc){
                String getterName = Reflector.getService().getMethodNameDependsCodeStyle(fl.getName(), codingStyle, Reflector.METHOD_TYPES.GET);
                try{
                    Method get = entity.getClass().getDeclaredMethod(getterName, null);
                    value = get.invoke(entity, null);
                }
                catch(NoSuchMethodException | InvocationTargetException | IllegalAccessException | IllegalArgumentException excOnFindAndRunGetter){
                    System.err.println("İlgili alanın verisi alınamadı : " + excOnFindAndRunGetter.toString());
                }
            }
            values.put(fl.getName(), value);
        }
        return values;
    }
    /**
     * Verilen nesnenin verilen alanlarını almak için kullanışlı bir yöntem
     * @param entity Sınıf örneği (nesne)
     * @param fieldNames Değeri alınmak istenen alanların isimleri
     * @param codingStyle Nesnenin 'getter' yönteminin kodlama biçimi
     * @return Verilen alanların değerleri veyâ boş bir {@code Map}
     * {@code null} parametre verilmesi durumunda {@code null} döndürülür 
     */
    public Map<String, Object> getValueOfFields(Object entity, List<String> fieldNames, CODING_STYLE codingStyle){
        ArrayList<Field> liFields = new ArrayList<Field>();
        if(entity == null || fieldNames == null)
            return null;
        if(fieldNames.isEmpty())
            return new HashMap<String, Object>();
        for(String s : fieldNames){
            try{
                Field fl = entity.getClass().getDeclaredField(s);
                if(fl != null)
                    liFields.add(fl);
            }
            catch(NoSuchFieldException | SecurityException exc){
                System.err.println("exc : " + exc.toString());
            }
        }
        Field[] takeds = new Field[liFields.size()];
        liFields.toArray(takeds);
        return getValueOfFields(entity, takeds, codingStyle);
    }
    /**
     * Uygulama kök dizini içerisindeki, yanî uygulamadaki sınıfları döndürür
     * @return Yüklenen sınıflardan oluşan bir {@code List} veyâ {@code null}
     */
    public List<Class> getClassesOnTheAppPath(){
        File appRoot = new File(ClassLoader.getSystemResource("").getPath());
        return getClassesOnThePath(appRoot);
    }
    /**
     * Verilen adresteki sınıfları döndürür (alt adresleri de tarar)
     * Dosya uzantısı '.class' olan dosyalar yüklenmeye çalışılır
     * @param path Sınıfların bulunduğu dizin aranacak
     * @return Sınıfların yüklendiği bir {@code List} veyâ {@code null}
     */
    public List<Class> getClassesOnThePath(File path){
        return getClassesOnTheRoot(path, null, true);
    }
    // ARKAPLAN İŞLEM YÖNTEMLERİ:
    /**
     * Verilen veriyi hedef sınıftaki nesneye zerk ederek nesne üretmeye çalışır
     * Şu an parametresiz yapıcı yöntemi bulunmayan sınıfın örneği üretilemiyor
     * {@code enum} değerler için "getter" erişim yöntemi aranmıyor; yanî enum 
     * değerin gizli olmaması lazım.
     * @param <T> Sınıf örneği istenen sınıf
     * @param targetClass Örneği istenen sınıf
     * @param data Sınıfın örneğine zerk edilmesi istenen özellik değerleri
     * @param tryToForceCastForParameterType Özelliğin veri tipi uyuşmadığında,
     * dönüşüm için ek yöntem uygulanmasını istiyorsanız {@code true} yapın
     * @param codeStyleNeededOnSearchMethod 'setter' yöntemine ihtiyaç duyulması
     * @param useGivenInstance Veriler verilen {@code instance} referansındaki
     * nesneye zerk edilecekse {@code true} olmalıdır.
     * @param instance {@code useGivenInstance} {@code true} ise hedef nesne
     * durumunda bu yöntemin hangi kodlama standardına göre aranacağı bilgisi
     * @return Verilen verilerin zerk edildiği sınıf örneği veyâ {@code null}
     */
    private <T> T pruduceNewInjectedObject(Class<T> targetClass, Map<String, ? extends Object> data, CODING_STYLE codeStyleNeededOnSearchMethod, boolean tryToForceCastForParameterType/*, boolean isIncludeNoParameterConstructor, List<Object> parameterForConstructor*/, boolean useGivenInstance, T instance){
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
        T obj = null;
        if(useGivenInstance && instance != null)// Verilerin zerk edileceği nesne kullanıcı tarafından verildiyse ve 'null' değilse;
            obj = instance;
        else// Diğer durumda yeni bir sınıf örneği oluştur
            obj = produceNewInstance(targetClass);
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
                                Object castedValue = fl.getType().cast(value);
                                value = castedValue;
                            }
                            catch(ClassCastException flCastException){
                                System.err.println("Şu alanın veri tipi verilen değerin veri tipiyle uyuşmuyor : " + fl.getName());
                                Object convertedValue = checkAndConvertListAndArray(value, fl);
                                if(convertedValue != null)
                                    value = convertedValue;
                                //Buraya continue; yazmıyorum; çünkü belki 'setter' yöntemi bu veri tipinden değişkeni parametre olarak kabûl ediyordur
                            }
                        }
                    }
                }
                try{
                    fl.set(obj, value);// Basit zerk : Erişim izni olmayan alanlar için çalışmaz!
                }
                catch(IllegalAccessException | IllegalArgumentException excOnBasicInjection){// Hedef özelliğe doğrudan erişim izni yok veyâ parametrenin veri tipi uyuşmuyor, diğer yöntemleri dene
//                    System.err.println("exc : " + excOnBasicInjection.toString());
                    // Veriyi hedef özelliğe zerk edebilmek için 'setter' yöntemi ara:
                    String methodName = getMethodNameDependsCodeStyle(fl.getName(), codeStyleNeededOnSearchMethod, METHOD_TYPES.SET);
                    ArrayList<Method> setters = new ArrayList<Method>();
                    try{
                        for(Method m : targetClass.getDeclaredMethods()){
                            if(m.getName().equals(methodName))
                                setters.add(m);
                        }
                    }
                    catch(SecurityException excOnTakingMethods){
                        System.err.println("Sınıfın yöntem isimleri alınamadı : " + excOnTakingMethods.toString());
                        continue;
                    }
                    if(setters.isEmpty())
                        continue;
                    for(int index = 0; index < setters.size(); index++){
                        try{
                            setters.get(index).invoke(obj, value);
//                            System.out.println("value : " + value);
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
    private List<Class> getClassesOnTheRoot(File root, String fullNameFromRoot, boolean isAppRoot){
        if(root == null)
            return null;
        List<Class> result = new ArrayList<Class>();
        String path = null;
        if(!isAppRoot)
            path = (fullNameFromRoot == null ? root.getName() : fullNameFromRoot + "." + root.getName());
        for(File fl : root.listFiles()){
            if(fl == null)
                continue;
            if(fl.isFile()){
                if(!fl.getName().endsWith(".class"))// Sadece '.class' uzantılı dosyaları yüklemeye çalış
                    continue;
                Class cls = null;
                try{
                    String fullName = (path == null ? fl.getName() : path + "." + fl.getName());
                    cls = ClassLoader.getSystemClassLoader().loadClass(fullName.substring(0, fullName.length() - 6));
                    if(cls != null)
                        result.add(cls);
                }
                catch(ClassNotFoundException exc){
                    System.err.println("Sınıf bulunamadı : " + exc.toString());
                }
            }
            if(fl.isDirectory()){
                List<Class> subList = getClassesOnTheRoot(fl, path, false);
                if(subList != null){
                    for(Class cls : subList){
                        if(cls != null)
                            result.add(cls);
                    }
                }
            }
        }
        return result;
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
    private HashMap<Class, Method> getNumberMethods(){
        if(numberMethods == null){
            numberMethods = new HashMap<Class, Method>();
            Class numberClass = Number.class;
            try{
                Method m = Number.class.getDeclaredMethod("doubleValue", null);
                numberMethods.put(double.class, m);
                numberMethods.put(Double.class, m);
                
                m = numberClass.getDeclaredMethod("intValue", null);
                numberMethods.put(int.class, m);
                numberMethods.put(Integer.class, m);
                
                m = numberClass.getDeclaredMethod("floatValue", null);
                numberMethods.put(float.class, m);
                numberMethods.put(Float.class, m);
                
                m = numberClass.getDeclaredMethod("shortValue", null);
                numberMethods.put(short.class, m);
                numberMethods.put(Short.class, m);
                
                m = numberClass.getDeclaredMethod("longValue", null);
                numberMethods.put(long.class, m);
                numberMethods.put(Long.class, m);
                Number ms;
                
                m = numberClass.getDeclaredMethod("byteValue", null);
                numberMethods.put(byte.class, m);
                numberMethods.put(Byte.class, m);
            }
            catch(NoSuchMethodException | SecurityException exc){
                System.err.println("Number sınıfı yöntemleri alınamadı");
            }
        }
        return numberMethods;
    }
    public List<String> getBasicDataTypes(){
        List<String> li = new ArrayList<String>();
        li.add("java.lang.String");
        li.add("int");
        li.add("double");
        li.add("float");
        li.add("short");
        li.add("boolean");
        li.add("long");
        li.add("char");
        li.add("byte");
        li.add("java.lang.Integer");
        li.add("java.lang.Double");
        li.add("java.lang.Float");
        li.add("java.lang.Boolean");
        li.add("java.lang.Long");
        li.add("java.lang.Short");
        li.add("java.lang.Character");
        li.add("java.lang.Byte");
        
        li.add("java.time.LocalDate");
        li.add("java.time.LocalDateTime");
        li.add("java.time.LocalTime");
        li.add("java.util.Date");
        li.add("java.sql.Date");
        li.add("java.lang.Number");
        return li;
    }
}