package ReflectorRuntime;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author Mehmet Âkif SOLAK
 * Düşük ve yüksek seviyeli nesne manipülasyonunu konforlu hâle getiren kitâplık
 * @version 3.0.0
 */
public class Reflector{
    private static Reflector serv;// service
    private HashMap<Class<?>, Class<?>> mapOfPrimitiveToWrapper;
    private final List<String> basicDataTypes = getBasicDataTypes();
    private DateTimeFormatter sqlAndIsoDFormatter;//sqlAndISODateFormatter : SQL târih veri tipini ayrıştırmak için...
    private DateTimeFormatter sqlAndIsoDTFormatter;//sqlAndISODateTimeFormatter : SQL târih saat veri tipini ayrıştırmak için kullanılıyor

    /**
     * 'getter' ve/veyâ 'setter' yöntemlerinin sınıf içerisinde
     * hangi isimle aranacağının bilinmesi için gerekli olan
     * kodlama biçimidir<br>
     * Misal, {@code "number"} özelliğine doğrudan zerk yapılamazsa,<br>
     * {@code "CAMEL_CASE"} seçilirse, {@code "setNumber"}<br>
     * {@code "SNAKE_CASE"} seçilirse, {@code "set_number"}<br>
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
    public Reflector(){}

// İŞLEM YÖNTEMLERİ:
    /**
     * Verilen dizi sınıfında bir dizi oluşturur<br>
     * Örnek kullanım:<br>
     * {@code String[] produced = produceArray(String[].class, 4);}<br>
     * @param <T> İstenen nesnenin tipi
     * @param classOfDataArray Dizisi istenen nesnenin sınıfı
     * @param length İstenen dizi uzunluğu
     * @return İstenen tipte dizi veyâ {@code null} döndürülür.
     */
    public <T> T[] produceArray(Class<T[]> classOfDataArray, int length){// Verilen dizi tipinde, verilen uzunlukta yeni bir değişken oluştur
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
     * Verilen dizi sınıfında dizi oluşturur, {@code Object} olarak döndürür<br>
     * Örnek kullanım:<br>
     * {@code int[] produced = produceArrayReturnAsObject(int[].class, 4);}<br>
     * Temel veri tiplerinin dizileri için kullanılabilmesi yazılmıştır<br>
     * {@code produceArray} sınıfı üst sınıfı {@code Object} olan diziler için
     * çalışmakta olup, temel veri tiplerinin dizileri için çalışmamaktadır
     * @param <T> İstenen nesnenin tipi
     * @param classOfDataArray Dizisi istenen nesnenin sınıfı
     * @param length İstenen dizi uzunluğu
     * @return İstenen tipte dizi (nesne olarak) veyâ {@code null} döndürülür.
     */
    public <T> T produceArrayReturnAsObject(Class<T> classOfDataArray, int length){// Yukarıdaki fonksiyonun aynısı; fakat temel veri tiplerinin dizisi için de çalışır, bi iznillâh..
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
     * Verilen sınıfın örneğini (nesnesini) oluşturur ve döndürür<br>
     * Şu sınıfların örneği üretilebilir:<br>
     * - Temel veri tipleri<br>
     * - Diziler<br>
     * - {@code Enum} tipindeki veriler<br>
     * - Parametresiz yapıcı yöntemi olan sınıflar<br>
     * - {@code List} için {@code ArrayList}, {@code Map} için {@code HashMap},<br>
     * - {@code SortedSet} için {@code TreeSet}, {@code Set} için {@code HashSet},<br>
     * - {@code Queue} için {@code LinkedList},<br>
     * - {@code Collection} için {@code ArrayList} üretilir<br>
     * @param <T> Örneği istenen sınıf, tip olarak
     * @param target Nesnesi üretilmek istenen sınıf
     * @return Verilen sınıfın ilklendirilmiş bir örneği veya {@code null}
     */
    public <T> T produceInstance(Class<T> target){
        if(target == null)
            return null;
        T obj = null;
        try{
            if(target.isPrimitive() || isWrapperClassOfBasic(target)){// Hedef temel veri tipi ise;
                Class<?> wrapper;
                if(target.isPrimitive())
                    wrapper = getWrapperClassFromPrimitiveClass(target);// Eğer temel veri tipi sınıfı ise, sarmalayıcı sınıfı edin
                else
                    wrapper = target;
                String parameterOfConstructor = getParameterForConstructorOfWrapperBasicClass(target);
                if(parameterOfConstructor != null){
                    obj = (T) wrapper.getConstructor(String.class).newInstance(parameterOfConstructor);
                }
                else if(target.equals(Character.class)){
                    obj = (T) wrapper.getConstructor(char.class).newInstance(' ');
                }
            }
            else if(target.isEnum()){// Hedef bir ENUM ise;
                obj = getProducedInstanceForEnumMain(target);
            }
            else if(target.isArray()){// Hedef diziyse;
                obj = target.cast(Array.newInstance(target.getComponentType(), 1));
            }
            else if(target.isInterface()){// Hedef bir arayüzse;
                if(target == List.class || target.getName().equals("java.util.Collection")){// List arayüzü ise ArrayList döndür
                    obj = (T) new ArrayList<Object>();
                }
                else if(target == Map.class){// Map arayüzü ise, HashMap döndür
                    obj = (T) new HashMap();
                }
                else if(target.getName().equals("java.util.SortedSet")){
                    obj = (T) new TreeSet<Object>();
                }
                else if(target.getName().equals("java.util.Queue")){
                    obj = (T) new LinkedList();
                }
                else if(target.getName().equals("java.util.Set")){
                    obj = (T) new HashSet();
                }
            }
            else{// Yapıcı yöntemi bulup, çalıştırarak yeni nesne elde etmeye çalış
                Constructor noParamCs = getConstructorForNoParameter(target);// İlk olarak parametresiz yapıcı yöntem ara
                if(noParamCs != null){
                    obj = (T) noParamCs.newInstance(null);
                }
            }
        }
        catch(NoSuchMethodException | SecurityException | InstantiationException | IllegalArgumentException | IllegalAccessException | InvocationTargetException exc){
            System.err.println("exc : " + exc.toString());
            return null;
        }
        return obj;
    }
    /**
     * Enum tipinde bir verinin oluşturulması için kullanılabilir<br>
     * Hedef Enum sınıfının ilk değeri getirilir<br>
     * Eğer Enum sınıfı boşsa {@code null} döndürülür<br>
     * @param <T> Enum tipinde veri tipi
     * @param targetEnum Hedef sınıf
     * @return Hedef Enum verilerinden ilk sırada olanı getirilir
     */
    public <T extends Enum> T getProducedInstanceForEnum(Class<T> targetEnum){
        return getProducedInstanceForEnumMain(targetEnum);
    }
//    public <T> T getProducedInstanceForInterface(Class<T> targetInterface){
//        if(targetInterface == null)
//            return null;
//        T obj = null;
//        if(targetInterface.isInterface()){
//            // uygulayıcı sınıflarından birisini seçip, uygulamalısın; fakat
//        }
//        return obj;
//    }
    /**
     * Verilen sınıfın temel veri tipi sınıfının sarmalayıcısı olup, olmadığı
     * sorgulanır<br>
     * @param cls İlgili sınıf
     * @return Temel veri tipi sarmalayıcısıysa {@code true}, değilse {@code false}
     */
    public boolean isWrapperClassOfBasic(Class<?> cls){
        if(cls == null)
            return false;
        for(Class<?> cl : getMapOfPrimitiveToWrapper().keySet()){
            if(getMapOfPrimitiveToWrapper().get(cl).equals(cls))
                return true;
        }
        return false;
    }
    /**
     * Verilen bilgilerle boyut bağımsız dizi oluşturun<br>
     * Dönüştürülemeyen bir eleman olursa {@code null} döndürülür<br>
     * Bu, varsayılan olarak böyledir; {@code ignoreMismatcElement} parametresi
     * {@code true} olarak verilirse, ilgili eleman listeye eklenmez, atlanır<br>
     * Atlanan verinin dizideki yeri için atama yapılmadığından 0 atanabilir
     * Bu fonksiyon en üst derinlikli elemanda veri tipi dönüşümünü ('casting')
     * desteklediğinden, verilerin dönüştürülmesi maksadıyla da kullanılabilir<br>
     * Misal, {@code List<List<Float>>} sınıfı {@code double[][]}'a dönüştürebilir<br>
     * @param <T> Hedef dizi sınıfını belirten tip
     * @param classOfDataArray Hedef sınıf, misal {@code int[][].class} gibi..
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @param doCastIfNeeded Eğer elemanın veri tipi, hedef veri tipiyle
     * uyumsuzsa ve dönüştürülmek isteniyorsa, bu bayrak {@code true} verilmeli
     * @param ignoreMismatchElement Dönüştürülemeyen elemanın görmezden
     * gelinmesini ifâde eden bayrak
     * @return İstenen verilerin zerk edildiği dizi örneği veyâ {@code null}
     */
    public <T> T produceInjectedArray(Class<T> classOfDataArray, Object data, boolean doCastIfNeeded, boolean ignoreMismatchElement){// Hedef diziye dönüştürme, derinlik bağımsız
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
                boolean isSucceed = false;
                if(dimensionIsEqualOne){
                    try{
                        Array.set(value, sayac, element);
                        isSucceed = true;
                    }
                    catch(IllegalArgumentException excOnMismatching){
                        if(doCastIfNeeded){// Veriyi dönüştürmeye çalış
                            Object casted = getCastedObject(classOfDataArray.getComponentType(), element);
                            if(casted != null){
                                Array.set(value, sayac, casted);
                                isSucceed = true;
                            }
                        }
                        if(!isSucceed){
                            if(ignoreMismatchElement)
                                continue;
                            else{
                                System.err.println("exc : " + excOnMismatching.toString());
                                return null;
                            }
                        }
                    }
                }
                else
                    Array.set(value, sayac,
                        produceInjectedArray(classOfDataArray.getComponentType(),
                            element, doCastIfNeeded, ignoreMismatchElement));
            }
            return (T) value;
        }
        catch(IllegalArgumentException | ClassCastException exc){
            System.err.println("exc : " + exc.toString());
        }
        return null;
    }
    /**
     * Verilen bilgilerle boyut bağımsız dizi oluşturun<br>
     * Dönüştürülemeyen bir eleman olursa {@code null} döndürülür<br>
     * Bu, varsayılan olarak böyledir; {@code ignoreMismatcElement} parametresi
     * {@code true} olarak verilirse, ilgili eleman listeye eklenmez, atlanırv
     * @param <T> Hedef dizi sınıfını belirten tip
     * @param classOfDataArray Hedef sınıf, misal {@code int[][].class} gibi..
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @param ignoreMismatchElement Dönüştürülemeyen elemanın görmezden
     * gelinmesini ifâde eden bayrak
     * @return İstenen verilerin zerk edildiği dizi örneği veyâ {@code null}
     */
    public <T> T produceInjectedArray(Class<T> classOfDataArray, Object data, boolean ignoreMismatchElement){
        return produceInjectedArray(classOfDataArray, data, false, ignoreMismatchElement);
    }
    /**
     * Verilen bilgilerle boyut bağımsız dizi oluşturun<br>
     * Dönüştürülemeyen bir eleman olursa {@code null} döndürülür<br>
     * Elemanların veri tipleri uyumsuzsa dönüştürülmeye çalışılmaz;<br>
     * Eğer elemanların dönüştürülmesini istiyorsanız, diğer yöntemi kullanın
     * {code @true} olarak verilirse, ilgili eleman listeye eklenmez, atlanır<br>
     * @param <T> Hedef dizi sınıfını belirten tip
     * @param classOfDataArray Hedef sınıf, misal {@code int[][].class} gibi..
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @return İstenen verilerin zerk edildiği dizi örneği veyâ {@code null}
     */
    public <T> T produceInjectedArray(Class<T> classOfDataArray, Object data){
        return produceInjectedArray(classOfDataArray, data, false);
    }
    /**
     * Verilen verilerle boyut bağımsız liste oluşturun<br>
     * @param data Veri {@code List} veyâ dizi ({@code Array}) biçiminde olmalı
     * @return İstenen verilerin zerk edildiği liste veyâ {@code null}
     */
    public List produceInjectedList(Object data){
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
                    value.add(sayac, produceInjectedList(element));
            }
            return value;
        }
        catch(IllegalArgumentException exc){
            System.err.println("exc : " + exc.toString());
        }
        return null;
    }
    /**
     * Verilen tiplerinin birbirine otomatik olarak dönüşebildiği denetleniyor<br>
     * Java otomatik sarmalama özelliğiyle sarmalanan sınıf - temel hâli eşleşir<br>
     * @param cls1 Sınıf - 1
     * @param cls2 Sınıf - 2
     * @return Eğer otomatik dönüşüyorsa {@code true}, değilse {@code false} 
     */
    public boolean isPairingAutomatically(Class<?> cls1, Class<?> cls2){
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
     * Sarmalayıcı sınıftan temel veri tipini elde edin<br>
     * @param wrapperClass Sarmalayıcı sınıf
     * @return Sarmalayıcı sınıfın karşılığı olan temel {@code "primitive"}
     * sınıf veyâ null döndürülür
     */
    public Class<?> getPrimitiveClassFromWrapper(Class<?> wrapperClass){
        Class<?> value = null;
        for(Class<?> cls : getMapOfPrimitiveToWrapper().keySet()){
            if(getMapOfPrimitiveToWrapper().get(cls).equals(wrapperClass)){
                value = cls;
                break;
            }
        }
        return value;
    }
    /**
     * Temel veri tipinin sarmalayıcı sınıfını döndürür<br>
     * @param primitiveClass Sarmalayıcısı istenen temel veri tipinin sınıfı
     * @return Verilen temel veri tipi sınıfının sarmalayıcısı veyâ {@code null}
     */
    public Class<?> getWrapperClassFromPrimitiveClass(Class<?> primitiveClass){
        return getMapOfPrimitiveToWrapper().get(primitiveClass);
    }
    /**
     * Verilen sınıfın parametresiz yapıcı yöntemini arar<br>
     * @param <T> Hedef sınıfı temsil eden sınıf
     * @param cls Hedef sınıf
     * @return Verilen sınıfın parametresiz yapıcı yöntemi veyâ {@code null}
     */
    public <T> Constructor<T> getConstructorForNoParameter(Class<T> cls){
        try{
            for(Constructor cs : cls.getDeclaredConstructors()){
                if(cs.getParameterCount() == 0)
                    return cs;
            }
            Class<?> clsSuper = cls.getSuperclass();
            if(clsSuper != null){
                if(!clsSuper.equals(Object.class))
                    return (Constructor<T>) getConstructorForNoParameter(clsSuper);
            }
        }
        catch(SecurityException | ClassCastException exc){
            System.err.println("Sınıf yapıcı yöntemleri aranırken hatâ alındı : " + exc.toString());
        }
        return null;
    }
    /**
     * Metîn biçiminde depolanan farklı veri tiplerindeki veriyi dönüştürür<br>
     * Dönüşümün esnekliği için bâzı özel durumlar ele alınmıştır:<br>
     * {@code Boolean} ve {@code boolean} veri tipi dönüştürmede 1 - 0 değerleri
     * {@code Boolean.TRUE} ve {@code Boolean.FALSE} değerleriyle eşleşir<br>
     * Veri, karakter tipinde isteniyorsa ilk karakteri alınır<br>
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
        if(target.isEnum()){
            return getEnumByData(target, data);
        }
        else if(isAboutDateTime(target)){
            return getDateObjectFromString(target, data);
        }
        else if(target.equals(File.class)){
            return (T) new File(data);
        }
        try{
            data = data.trim();// Boşluklar varsa kaldır
            Object casted = null;
            if(isAboutNumber(target)){
                casted = getCastedNumberFromString(target, data);
                return (casted == null ? null : (T) casted);
            }
            else if(target.equals(Boolean.class) || target.equals(boolean.class)){
                String preProcessed = data.toLowerCase();
                switch(preProcessed){
                    case "true" : {
                        casted = Boolean.TRUE;
                        break;
                    }
                    case "false" : {
                        casted = Boolean.FALSE;
                        break;
                    }
                    case "1" : {
                        casted = Boolean.TRUE;
                        break;
                    }
                    case "0" : {
                        casted = Boolean.FALSE;
                        break;
                    }
                }
            }
            else if(target.equals(Character.class) || target.equals(char.class))
                casted = data.charAt(0);// İlk karakter alınıyor
            return (T) casted;
        }
        catch(ClassCastException exc){
            System.err.println("Veri, istenen veri tipine dönüştürülemedi : " + exc.toString());
        }
        return null;
    }
    /**
     * SQL ve ISO biçimindeki târih, târih - zamân ve zamân verisini hedef
     * tipe dönüştürmek için kullanılır<br>
     * Verilen târihin daha kapsayıcı olduğu durumda alt birime dönüştürülebilir<br>
     * @param <T> Hedef sınıfı temsil eden tip
     * @param target Hedef sınıf, şunlardan birisi olabilir:<br>
     * - {@code java.time.LocalDateTime}<br>
     * - {@code java.time.LocalDate}<br>
     * - {@code java.time.LocalTime}v
     * - {@code java.util.Date}v
     * Şu formattaki veriler tanınır:<br>
     * "[yyyy[-]MM[-]-dd]['T' | ' '][HH:mm:ss][.S{0,1,2,3,4,5,6}]<br>
     * Misal,şu veriler veyâ bu verilerin alt parçaları (târih - saat) tanınır:<br>
     * "2025-05-12 12:34:43" : SQL biçimi<br>
     * "20250512 12:34:43" : SQL biçimiv
     * "20250512 12:34:43.215353" : SQL biçimi<br>
     * "2025-05-12T12:34:43" : ISO biçimi<br>
     * "2025-05-12T12:34:43.215353" : ISO biçimi<br>z
     * @param data ISO veyâ SQL biçimindeki metîn
     * @return İstenen tipte târih - zamân verisi veyâ {@code null}
     */
    public <T> T getDateObjectFromString(Class<T> target, String data){//2025-05-12 verisinden 2025-05-12 00:00:00 verisini üretecek şekilide dayanıklılık ekle
        if(target == null || data == null)
            return null;
        if(data.isEmpty())
            return null;
        T result = null;// Sonuç
        try{
            TemporalAccessor raw = getSQLAndISODTFormatter().parse(data);// Ayrıştırma
            if(target.equals(LocalDate.class))
                result = (T) LocalDate.from(raw);
            else if(target.equals(LocalDateTime.class))
                result = (T) LocalDateTime.from(raw);
            else if(target.equals(LocalTime.class))
                result = (T) LocalTime.from(raw);
            else if(target.equals(Date.class))
                result = (T) Date.from(LocalDateTime.from(raw).toInstant(ZoneOffset.of("Z")));
            else if(target.equals(java.sql.Date.class))
                result = (T) java.sql.Date.valueOf(LocalDate.from(raw));
        }
        catch(IllegalArgumentException | NullPointerException | DateTimeException exc){
            System.err.println("Târih saat verisini parçalama işlemi başarısız : " + exc.toString());
        }
        return result;
    }
    /**
     * Verilen metindeki sayıyı kullanarak verilen tipte sayı nesnesi üretir<br>
     * Verilen sayı tipi, sayı depolayabilen tüm temel veri tiplerinden birisi
     * veyâ {@code java.lang.Number} sınıfının alt elemanı olmalıdır<br>
     * @param <T> Hedef sayı sınıfını simgeleyen tip
     * @param target Hedef sayı sınıfı
     * @param data Sayı içeren metîn
     * @return İstenilen tipte sayı veyâ {@code null}
     */
    public <T> T getCastedNumberFromString(Class<T> target, String data){
        try{
            Object casted = null;
            data = data.trim();// Boşluklar varsa kaldır
            if(target.equals(Integer.class) || target.equals(int.class))// Tamsayı ise;
                casted = Integer.valueOf(data);
            else if(target.equals(Double.class) || target.equals(double.class))
                casted = Double.valueOf(data);
            else if(target.equals(Float.class) || target.equals(float.class))
                casted = Float.valueOf(data);
            else if(target.equals(Byte.class) || target.equals(byte.class))
                casted = Byte.valueOf(data);
            else if(target.equals(Long.class) || target.equals(long.class))
                casted = Long.valueOf(data);
            else if(target.equals(Short.class) || target.equals(short.class))
                casted = Short.valueOf(data);
            else if(target.equals(BigDecimal.class))
                casted = new BigDecimal(data);
            else if(target.equals(BigInteger.class))
                casted = new BigInteger(data);
            return (T) casted;
        }
        catch(ClassCastException | NumberFormatException exc){// Kesir değeri 0 olanların tamsayıya çevrilebilmesi için ilâve işlem
            if(target == Integer.class || target.equals(int.class) ||
                target == Long.class || target.equals(long.class) ||
                target == Byte.class || target.equals(byte.class) ||
                target == Short.class || target.equals(short.class) || target.equals(BigInteger.class)){
                try{
                    Double asDouble = Double.valueOf(data);
                    if(asDouble != null){
                        if(asDouble - Math.ceil(asDouble) == 0){// Kesir kısmındaki değer 0 ise;
                            Long ll = Math.round(asDouble);
                            if(target.equals(Long.class) || target.equals(long.class))
                                return (T) ll;
                            String asStr = String.valueOf(ll);
                            if(target.equals(Integer.class) || target.equals(int.class))
                                return (T) Integer.valueOf(asStr);
                            else if(target.equals(BigInteger.class))
                                return (T) new BigInteger(String.valueOf(ll));
                            else if(target.equals(Short.class) || target.equals(short.class))
                                return (T) Short.valueOf(asStr);
                            else{
                                return (T) Byte.valueOf(asStr);
                            }
                        }
                    }
                }
                catch(ClassCastException | NumberFormatException excOn2nd){
                    System.err.println("Veri hiçbir şekilde hedef sayı tipine çevrilemiyor");
                }
            }
            System.err.println("İstenen veri tipine dönüştürülemedi : " + exc.toString());
        }
        return null;
    }
    /**
     * Verilerin birbirine dönüştürülebildiği durumlar için dönüşüm desteklenir<br>
     * İlâveten, {@code Enum} verisi metîn biçimindeyse, karşılığı döndürülür<br>
     * Normalde birbirine dönüştürülebilir olan nesnelerin dizilerini dönüştürme
     * özelliği vardır<br>
     * {@code Array} ve {@code List} arasında dönüşümü destekler<br>
     * Bu ilâve özellikler Java tarafından desteklenmeyen pek çok dönüşümün
     * yapılabilmesini sağlar<br>
     * @param <T> Hedef, temsil eden veri tipi
     * @param targetClass Hedef sınıf
     * @param value Veri
     * @return Hedef tipinden bir nesneye dönüştürülmüş veri, veyâ {@code null}
     */
    public <T> T getCastedObject(Class<T> targetClass, Object value){// Verilen değeri hedef tipe dönüştürme
        if(value == null)
            return null;
        if(isAboutListArrayConverting(targetClass, value)){
            Object casted = checkAndConvertListAndArrayFixed(value, targetClass);
            if(casted != null){
                try{
                    return (T) casted;
                }
                catch(ClassCastException exc){}
            }
        }
        T casted = getCastedObjectFromString(String.valueOf(value), targetClass);
        if(casted == null){
            try{
                casted = targetClass.cast(value);
            }
            catch(ClassCastException exc){
                System.err.println("Dönüşüm hatâsı : " + exc.toString());
            }
        }
        return casted;
    }
    /**
     * Verilen veriyi hedef sınıftaki nesneye zerk ederek nesne üretmeye çalışır<br>
     * Şu an parametresiz yapıcı yöntemi bulunmayan sınıfın örneği üretilemiyor<br>
     * {@code enum} değerler için "getter" erişim yöntemi aranmıyor; yanî enum 
     * değerin gizli olmaması lazım.<br>
     * @param <T> Sınıf örneği istenen sınıf
     * @param targetClass Örneği istenen sınıf
     * @param data Sınıfın örneğine zerk edilmesi istenen özellik değerleri
     * @param codeStyleNeededOnSearchMethod 'setter' yöntemine ihtiyaç duyulması
     * durumunda bu yöntemin hangi kodlama standardına göre aranacağı bilgisi
     * @return Verilen verilerin zerk edildiği sınıf örneği veyâ {@code null}
     */
    public <T> T produceInjectedObject(Class<T> targetClass, Map<String, ? extends Object> data, CODING_STYLE codeStyleNeededOnSearchMethod){
        return produceInjectedObject(targetClass, data, codeStyleNeededOnSearchMethod, true, false, null, true, true);
    }
    /**
     * Verilen alan({@code Field} değerlerini verilen nesneye zerk eder(aktarır)<br>
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
        return produceInjectedObject((Class<T>) targetObject.getClass(), data, codeStyleNeededOnSearchMethod, true, true, targetObject, true, true);
    }
    /**
     * Verilen dizi sınıfına bakarak dizi boyutunu döndürür<br>
     * @param cls Dizi sınıfı, misal {@code int[][].class} gibi..
     * @return Dizi boyutu, sınıf dizi sınıfı değilse veyâ {@code null} ise 0
     */
    public int getDimensionOfArray(Class<?> cls){
        if(cls == null)
            return 0;
        return (cls.getTypeName().split("\\[").length - 1);
    }
    /**
     * Verilen listenin elemanlarını tarayarak listenin derînliğini araştırır<br>
     * Eğer dizi elemanı bir liste ise, derinlik bir arttırılır ve o da taranır<br>
     * @param list Liste
     * @return Listenin derînliği, liste {@code null} ise {@code 0} döndürülür
     */
    public int getDimensionOfList(List<?> list){
        if(list == null)
            return 0;
        return findDepthWhole(list, 1);
    }
    /**
     * Verilen veri tipinin temel veri tipi veyâ sarmalayıcısı olup, olmadığını
     * sorgular<br>
     * @param dataTypeName Veri tipi tam ismi
     * @return Verilen veri tipi temel ise {@code true}, değilse {@code false}
     */
    public boolean isDataTypeBasic(String dataTypeName){
        for(String s : this.basicDataTypes){
            if(s.equals(dataTypeName))
                return true;
        }
        return false;
    }
    /**
     * Verilen listenin elemanlarını tarayarak listenin derînliği araştırılır<br>
     * Eğer dizi elemanı bir liste ise, derinlik bir arttırılır ve o da taranır<br>
     * @param list Liste
     * @param depth Derinlik, yöntemi çağırırken {@code 1} değeri verilmeli
     * @return Listenin derînliği, liste {@code null} ise {@code 0} döndürülür
     */
    public int findDepthWhole(List<?> list, int depth){
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
     * dizi şeklindeki bir veriyi zerk edebilmek için gereken dönüşümü yapar<br>
     * @param value Hedef alana zerk edilmek istenen veri, liste veyâ dizi
     * @param target Hedef alan
     * @return Hedef alan ile uyumlu biçimdeki veri veyâ {@code null}
     */
    public Object checkAndConvertListAndArrayFixed(Object value, Class<?> target){
        // İki soruna çözüm aranıyor:
        // 1) value = List ve element = Array ise uyumluluk arayıp, atama yapma
        // 2) value = Array ve element = List ise, uyumluluk arayıp, atama yapma
        if(value == null || target == null)
            return null;
        boolean isValueAnArray = value.getClass().isArray();
        boolean isTargetAnArray = target.isArray();
        boolean isValueAnList = false;
        boolean isTargetAnList = false;
        if(!isValueAnArray){
            if(value instanceof List)
                isValueAnList = true;
        }
        if(!isTargetAnArray){
            try{
                Class<?> casted = target.asSubclass(List.class);
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
            int dimensionOfTarget = getDimensionOfArray(target);// Hedef dizinin boyutu
            int dimensionOfValue = (isValueAnArray ? getDimensionOfArray(value.getClass()) : getDimensionOfList((List) value));
            if(dimensionOfValue > dimensionOfTarget)// Hedef ile veri arasında dizi boyutu uyuşmazlığı var
                return null;
            return produceInjectedArray(target, value, true, false);
        }
        else{// Hedef bir liste ise...
            return produceInjectedList(value);
        }
    }
    /**
     * Liste veyâ dizi şeklindeki bir alana doğrudan zerk edilemeyen liste veyâ
     * dizi şeklindeki bir veriyi zerk edebilmek için gereken dönüşümü yapar<br>
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
                Class<?> casted = target.getType().asSubclass(List.class);
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
            int dimensionOfValue = (isValueAnArray ? getDimensionOfArray(value.getClass()) : getDimensionOfList((List) value));
            if(dimensionOfValue > dimensionOfTarget)// Hedef ile veri arasında dizi boyutu uyuşmazlığı var
                return null;
            return produceInjectedArray(target.getType(), value, true, false);
        }
        else{// Hedef bir liste ise...
            return produceInjectedList(value);
        }
    }
    /**
     * İsmi verilen bir özelliğin ('attribute') 'getter' veyâ 'setter' yöntem
     * ismini döndürür<br>
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
     * Verilen sınıfın alanlarını alır<br>
     * Üst sınıf alanlarının da alınması için {@code takeFieldsInSuperClasses}
     * {@code true} olmalıdır<br>
     * @param cls Hedef sınıf
     * @param scanSuperClasses Üst sınıfların taranması durumu
     * @return Alanları içeren {@code List} nesnesi
     */
    public List<Field> getFields(Class<?> cls, boolean scanSuperClasses){
        if(cls == null)
            return null;
        List<Field> li = new ArrayList<Field>();
        Map<String, Field> map = new HashMap<String, Field>();
        getFieldsMain(cls, map, scanSuperClasses);
        li.addAll(map.values());
        return li;
    }
    /**
     * Verilen sınıfın verilen alanlarını alır<br>
     * Üst sınıf alanlarının da taranması için {@code takeFieldsInSuperClasses}
     * {@code true} olmalıdır<br>
     * {@code givenFieldNames} boşsa geriye boş {@code List<Field>} döndürülür
     * @param cls Hedef sınıf
     * @param givenFieldNames Alınması istenen alan isimleri
     * @param scanSuperClasses Üst sınıfların taranması durumu
     * @return Alanları içeren {@code List} nesnesi
     */
    public List<Field> getFields(Class<?> cls, List<String> givenFieldNames, boolean scanSuperClasses){
        if(cls == null || givenFieldNames == null)
            return null;
        List<Field> li = new ArrayList<Field>();
        if(givenFieldNames.isEmpty())
            return li;
        Map<String, Field> map = new HashMap<String, Field>();
        getFieldsMain(cls, map, scanSuperClasses);
        for(String name : givenFieldNames){
            Field fl = map.get(name);
            if(fl != null)
                li.add(fl);
        }
        return li;
    }
    /**
     * Verilen nesnenin, verilen alanlarının değerleri alınır ve döndürülür<br>
     * Eğer {@code fieldNames} {@code null} ise tüm alanların değerleri alınır<br>
     * Üst sınıfların taranması {@code scanSuperClasses} parametresine bağlıdır
     * Hatâ oluşursa {@code null} döndürülür<br>
     * @param entity Alan (field) değerleri alınmak istenen nesne
     * @param fieldNames İstenen alanların isimleri
     * @param codingStyle Gerektiğinde "getter" metodu için kodlama biçimi
     * @param forceAccessibility Erişimin zorlanmasını ifâde eden parametre
     * @param scanSuperClasses Üst sınıfların taranmasını ifâde eden parametre
     * @return Alan ve değerlerden oluşan bir {@code Map} veyâ {@code null}
     */
    public Map<String, Object> getValueOfFields(Object entity, List<String> fieldNames,
        CODING_STYLE codingStyle, boolean forceAccessibility, boolean scanSuperClasses){
        if(entity == null || codingStyle == null)
            return null;
        List<Field> liFields;
        
        // Alan ismi verilmemişse tüm alan isimlerini al (üst sınıf taraması
        // scanSuperClasses parametresine bağlı):
        if(fieldNames == null)
            liFields = getFields(entity.getClass(), scanSuperClasses);
        // Verilen isimdeki alanları al (üst sınıftakilerin alınması
        // scanSuperClasses parametresine bağlı):
        else
            liFields = getFields(entity.getClass(), fieldNames, scanSuperClasses);
        
        // Alınacak alan yoksa;
        boolean turnEmpty = false;
        if(liFields == null)
            turnEmpty = true;
        if(liFields.isEmpty())
            turnEmpty = true;
        if(turnEmpty)
            return new HashMap<String, Object>();
        Field[] takens = new Field[liFields.size()];
        liFields.toArray(takens);
        return getValueOfFields(entity, takens, codingStyle, forceAccessibility);
    }
    /**
     * Verilen nesnenin, verilen alanlarının değerleri alınır ve döndürülür<br>
     * Eğer {@code fields} {@code null} ise {@code null} döndürülür<br>
     * Üst sınıfların taranması {@code scanSuperClasses} parametresine bağlıdır<br>
     * Alınan alan verisi {@code null} olsa bile döndürülen haritaya eklenir<br>
     * @param entity Verisi alınmak istenen nesne
     * @param fields Verisi alınmak istenen alanlar
     * @param codingStyle Gerektiğinde "getter" metodu için kodlama biçimi
     * @param forceAccessibility Erişimi zorlamayı ifâde eden parametre
     * @return Alan ve değerlerden oluşan bir {@code Map} veyâ {@code null}
     */
    public Map<String, Object> getValueOfFields(Object entity, Field[] fields,
            CODING_STYLE codingStyle, boolean forceAccessibility){
        if(entity == null || fields == null || codingStyle == null)
            return null;
        Map<String, Object> values = new HashMap<String, Object>();
        for(Field fl : fields){
            boolean fetchByMethod = false;
            if(fl == null)
                continue;
            try{// Alan üzerinden veri çekmeye çalış:
                Object val = fl.get(entity);
                values.put(fl.getName(), val);
            }
            catch(ExceptionInInitializerError | NullPointerException | IllegalArgumentException exc){
                fetchByMethod = true;
//                System.err.println("exc : " + exc.toString());
            }
            catch(IllegalAccessException excIllegal){// İzinsiz erişim hatâsı alındıysa;
                if(forceAccessibility){// Erişim zorlaması yapılmak istendiyse;
                    try{
                        fl.setAccessible(true);
                        Object val = fl.get(entity);
                        values.put(fl.getName(), val);
                        fl.setAccessible(false);
                    }
                    catch(ExceptionInInitializerError | NullPointerException | IllegalArgumentException | IllegalAccessException exc2nd){
                        fetchByMethod = true;
//                        System.err.println("exc2nd : " + exc2nd.toString());
                    }
                    catch(SecurityException excOnAccessibility){
                        fetchByMethod = true;
//                        System.err.println("excOnAccessibility : " + excOnAccessibility.toString());
                    }
                }
                else
                    fetchByMethod = true;
            }
            
            // Eğer veri, alan üzerinden alınamıyorsa, metot üzerinden almayı dene:
            if(fetchByMethod){
                Method m = getSpecialMethod(entity.getClass(),fl.getName(), METHOD_TYPES.GET, codingStyle);
                Object val = invokeMethod(entity, m, null, forceAccessibility);
                values.put(fl.getName(), val);
            }
        }
        return values;
    }
    /**
     * Verilen nesneyi kullanarak verilen metodu çalıştırır<br>
     * Metot girdileri {@code inputs} parametresiyle sırasıyla belirtilmelidir<br>
     * Eğer metot girdi almıyorsa {@code inputs}'a {@code null} verilmelidir<br>
     * Erişimi zorlamak için {@code forceAccessibility} parametresi kullanılır<br>
     * Metodun döndürdüğü değer geriye döndürülür<br>
     * Hatâ durumunda geriye {@code null} döndürülebildiği gibi, metottan da
     * {@code null} değeri döndürülebilir<br>
     * Bu belirsizliği kaldırmak için diğer {@code invokeMethod()}'a bakınız<br>
     * İşlemin başarıyla tamâmlanıp, tamâmlanmadığı verilen {@code result}
     * nesnesine "result" anahtarıyla yazılır<br>
     * {@code result} parametresine boş bir {@code Map} vermeniz kâfî<br>
     * @param entity Metodun çalıştırılacağı nesne
     * @param target Metodun kendisi
     * @param inputs Metoda verilecek girdi dizisi
     * @param forceAccessibility Erişimi zorlamayı belirten parametre
     * @return Metottan dönen sonuç veyâ {@code null}
     */
    public Object invokeMethod(Object entity, Method target, Object[] inputs, boolean forceAccessibility){
        return invokeMethod(entity, target, inputs, forceAccessibility, null);
    }
    /**
     * Verilen nesneyi kullanarak verilen metodu çalıştırır<br>
     * Metot girdileri {@code inputs} parametresiyle sırasıyla belirtilmelidir<br>
     * Eğer metot girdi almıyorsa {@code inputs}'a {@code null} verilmelidir<br>
     * Erişimi zorlamak için {@code forceAccessibility} parametresi kullanılır<br>
     * Metodun döndürdüğü değer geriye döndürülür<br>
     * Hatâ durumunda geriye {@code null} döndürülebildiği gibi, metottan da
     * {@code null} değeri döndürülebilir<br>
     * Buradaki belirsizliği kaldırmak için {@code result} parametresiyle
     * belirtilen parametre kullanılır<br>
     * İşlemin başarıyla tamâmlanıp, tamâmlanmadığı verilen {@code result}
     * nesnesine "result" anahtarıyla yazılır<br>
     * {@code result} parametresine boş bir {@code Map} vermeniz kâfî<br>
     * @param entity Metodun çalıştırılacağı nesne
     * @param target Metodun kendisi
     * @param inputs Metoda verilecek girdi dizisi
     * @param forceAccessibility Erişimi zorlamayı belirten parametre
     * @param result İşlem başarılı olursa buna {@code Boolean.TRUE} yazılır
     * @return Metottan dönen sonuç veyâ {@code null}
     */
    public Object invokeMethod(Object entity, Method target, Object[] inputs, boolean forceAccessibility, Map<String, Object> result){
        if(target == null)// entity 'null' olabilir (statik metot ise)
            return null;
        Object res = null;
        if(result != null)
            result.put("result", Boolean.FALSE);
        try{
            res = target.invoke(entity, inputs);
            if(result != null)
                result.put("result", Boolean.TRUE);
        }
        catch(InvocationTargetException | IllegalArgumentException | SecurityException exc){
//            System.err.println("exc : " + exc.toString());
        }
        catch(IllegalAccessException exc){
            if(forceAccessibility){
                try{
                    target.setAccessible(true);
                    res = target.invoke(entity, inputs);
                    if(result != null)
                        result.put("result", Boolean.TRUE);
                    target.setAccessible(false);
                }
                catch(SecurityException | InvocationTargetException | IllegalArgumentException |IllegalAccessException exc2nd){
//                    System.err.println("exc2nd : " + exc2nd.toString());
                }
            }
        }
        return res;
    }
    /**
     * {@code methodNames} parametresi {@code null} ise tüm metotlar alınır<br>
     * Üst sınıfların taranması {@code scanSuperClasses} parametresine bağlıdır<br>
     * Üst sınıflar taransa bile çakışma durumunda alt sınıftaki metot geçerlidir.<br>
     * @param cls Hedef sınıf
     * @param scanSuperClasses Üst sınıflar taranacak mı
     * @return Metot listesi
     */
    public List<Method> getMethods(Class<?> cls, boolean scanSuperClasses){
        if(cls == null)
            return null;
        List<Method> li = new ArrayList<Method>();
        Map<String, Method> map = new HashMap<String, Method>();
        getMethodsMain(cls, map, scanSuperClasses);
        li.addAll(map.values());
        return li;
    }
    /**
     * "getter" gibi özel metotları almak için kullanılan bir metottur<br>
     * Eğer ilgili alan için aranan metot sınıfta yoksa, üst sınıflara bakılır<br>
     * Bu üst tarama işleminde kök sınıf {@code Object} dâhil değildir<br>
     * @param cls Hedef sınıf
     * @param fieldName Alanın ismi
     * @param methodType Aranan metodun tipi
     * @param codingStyle Metot isminin çıkartılabilmesi için kodlama biçimi
     * @return Aranan alanın verilen tipteki metodu veyâ {@code null}
     */
    public Method getSpecialMethod(Class<?> cls, String fieldName, METHOD_TYPES methodType,
        CODING_STYLE codingStyle){
        return getSpecialMethod(cls, fieldName, methodType, codingStyle, true);
    }
    /**
     * "getter" gibi özel metotları almak için kullanılan bir metottur<br>
     * Üst sınıfın taranması {@code scanSuperClasses} parametresine bağlıdır<br>
     * Bu üst tarama işleminde kök sınıf {@code Object} dâhil değildir<br>
     * @param cls Hedef sınıf
     * @param fieldName Alanın ismi
     * @param methodType Aranan metodun tipi
     * @param codingStyle Metot isminin çıkartılabilmesi için kodlama biçimi
     * @param scanSuperClasses Üst sınıfların taranmasını ifâde eden parametre
     * @return Aranan alanın verilen tipteki metodu veyâ {@code null}
     */
    public Method getSpecialMethod(Class<?> cls, String fieldName, METHOD_TYPES methodType,
        CODING_STYLE codingStyle, boolean scanSuperClasses){
        if(cls == null || fieldName == null || methodType == null)
            return null;
        String methodName = getMethodNameDependsCodeStyle(fieldName, codingStyle, methodType);
        Method found = null;
        try{
            Method[] mS = cls.getDeclaredMethods();
            for(Method m : mS){
                if(m.getName().equals(methodName))
                    found = m;
            }
            if(found == null){
                if(scanSuperClasses){
                    Class<?> clsSuper = cls.getSuperclass();
                    if(clsSuper != null){
                        if(!clsSuper.equals(Object.class))
                            return getSpecialMethod(cls.getSuperclass(), fieldName, methodType, codingStyle);
                    }
                }
            }
            return found;
        }
        catch(SecurityException exc){
            System.err.println("exc : " + exc.toString());
            return null;
        }
    }
    /**
     * Verilen listedeki isimlerdeki metotlar aranır<br>
     * Verilen isim listesi boşsa boş {@code List<Field>} nesnesi döndürülür<br>
     * Üst sınıfları tarama işlemi {@code scanSuperClasses} parametresine bağlıdır<br>
     * @param cls Hedef sınıf
     * @param givenMethodNames İstenen metotların isimleri
     * @param scanSuperClasses Üst sınıfın taranıp, taranmayacağı bilgisi
     * @return İstenen metotları içeren {@code List} nesnesi veyâ {@code null}
     */
    public List<Method> getMethods(Class<?> cls, List<String> givenMethodNames, boolean scanSuperClasses){
        if(cls == null || givenMethodNames == null)
            return null;
        List<Method> res = new ArrayList<Method>();
        if(givenMethodNames.isEmpty())
            return res;
        Map<String, Method> map = new HashMap<String, Method>();
        getMethodsMain(cls, map, scanSuperClasses);
        for(String s : givenMethodNames){
            Method m = map.get(s);
            if(m != null)
                res.add(m);
        }
        return res;
    }
    /**
     * Uygulama kök dizini içerisindeki, yanî uygulamadaki sınıfları döndürür<br>
     * @return Yüklenen sınıflardan oluşan bir {@code List} veyâ {@code null}
     */
    public List<Class<?>> getClassesOnTheAppPath(){
        String path = ClassLoader.getSystemResource("").getPath();
        File appRoot = null;
        try{
            appRoot = new File(URLDecoder.decode(path, "UTF8"));
        }
        catch(UnsupportedEncodingException exc){
            return null;
        }
        if(appRoot == null)
            return null;
        return getClassesOnThePath(appRoot);
    }
    /**
     * Verilen adresteki sınıfları döndürür (alt adresleri de tarar)<br>
     * Dosya uzantısı '.class' olan dosyalar yüklenmeye çalışılır<br>
     * @param path Sınıfların bulunduğu dizin aranacak
     * @return Sınıfların yüklendiği bir {@code List} veyâ {@code null}
     */
    public List<Class<?>> getClassesOnThePath(File path){
        return getClassesOnTheRoot(path, null, true);
    }
    /**
     * Verilen veriye dayanarak ilgili Enum verisini getirir<br>
     * Verinin {@code String.valueOf()} ile alınan değeri hedefteki değerlerin
     * aynı fonksiyonla alınan değerine eşit olmalıdır;<br>
     * bu durumda {@code Enum} tipinde istediğiniz değişken verisini
     * {@code String} tipinde verebilirsiniz.<br>
     * @param <T> Hedef sınıfı temsîl eden sınıf
     * @param target Hedef {@code Enum} sınıfı
     * @param data Veri, {@code Enum} veyâ {@code String} tipinde olabilir
     * @return İstenen {@code Enum} değeri veyâ {@code null}
     */
    public <T> T getEnumByData(Class<T> target, Object data){
        if(target == null || data == null)
            return null;
        if(!target.isEnum())
            return null;
        T value = null;
        try{
            for(T curr : target.getEnumConstants()){
                if(String.valueOf(curr).equals(String.valueOf(data)))
                    value = curr;
            }
        }
        catch(Exception exc){
            System.err.println("exc : " + exc.toString());
        }
        return value;
    }
    /**
     * Verilen târih, târih - saat ve saat nesnesini SQL tarzı metne çevirir<br>
     * Yalnızca kabûl edilen zamân sınıfları için işlem yapılır<br>
     * @param value Zamân nesnesi, şu tipler kabûl edilir:<br>
     * - {@code java.time.LocalDateTime}<br>
     * - {@code java.time.LocalDate}<br>
     * - {@code java.time.LocalTime}<br>
     * - {@code java.util.Date}<br>
     * - {@code java.sql.Date}<br>
     * @return SQL biçiminde zamân değeri metni veyâ {@code null}
     */
    public String getDateTimeTextAsSQLStyle(Object value){
        if(value == null)
            return null;
        Class<?> src = value.getClass();
        String result = null;
        DateTimeFormatter sqlDFrm = new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter();//sqlDateFormatter
        DateTimeFormatter sqlTFrm = DateTimeFormatter.ISO_TIME;
        DateTimeFormatter sqlLDTFrm = new DateTimeFormatterBuilder().append(sqlDFrm)
                .appendLiteral(' ').append(sqlTFrm).toFormatter();
        try{
            if(src.equals(LocalDateTime.class)){
                result = ((LocalDateTime) value).format(sqlLDTFrm);
            }
            else if(src.equals(LocalDate.class))
                result = ((LocalDate) value).format(sqlDFrm);

            else if(src.equals(LocalTime.class))
                result = ((LocalTime) value).format(sqlTFrm);
            else if(src.equals(Date.class)){
                result = LocalDateTime.ofInstant(((Date) value).toInstant(),
                        ZoneOffset.of("Z")).format(sqlLDTFrm);
            }
            else if(src.equals(java.sql.Date.class))
                result = ((java.sql.Date) value).toLocalDate().format(sqlDFrm);
        }
        catch(IllegalArgumentException | NullPointerException | DateTimeException exc){
            System.err.println("Verilen zamân nesnesi hedef biçime çevrilemedi : " + exc.toString());
        }
        return result;
    }
    /**
     * Verilen bildirim (notasyon, {@code Annotation}) ile süslenmiş alanların
     * isimlerini getirir<br>
     * Herhangi bir güvenlik hatâsı alınırsa veyâ yanlış parametre verilmişse
     * {@code null} döndürülür<br>
     * @param <T> Bildirimi ifâde eden tip
     * @param target Hedef sınıf
     * @param annotation Bildirimin sınıfı
     * @return Verilen bildirimle süslenmiş alanların isimlerini içeren liste
     */
    public <T extends Annotation> List<String> getAnnotatedFieldsByGivenAnnotation(Class<?> target, Class<T> annotation){
        return getAnnotatedFieldsByGivenAnnotation(target, annotation, false);
    }
    /**
     * Verilen bildirim (notasyon, {@code Annotation}) ile süslenmiş alanın
     * ismini getirir<br>
     * Herhangi bir güvenlik hatâsı alınırsa veyâ yanlış parametre verilmişse
     * {@code null} döndürülür<br>
     * @param <T> Bildirimi ifâde eden tip
     * @param target Hedef sınıf
     * @param annotation Bildirimin sınıfı
     * @return Verilen bildirimle süslenmiş alanların isimlerini içeren liste
     */
    public <T extends Annotation> List<String> getAnnotatedFieldByGivenAnnotation(Class<?> target, Class<T> annotation){
        return getAnnotatedFieldsByGivenAnnotation(target, annotation, true);
    }
    /**
     * Verilen sınıf içerisinde ilgili alanın olup, olmadığı sorgulanır<br>
     * Herhangi bir güvenlik hatâsı alınması durumunda {@code false} döndürülür<br>
     * @param cls Hedef sınıf
     * @param nameOfIdField Hedef alan ismi
     * @return İlgili alan mevcutsa {@code true}, aksi hâlde {@code false}
     */
    public boolean checkFieldIsExist(Class<?> cls, String nameOfIdField){
        if(cls == null || nameOfIdField == null)
            return false;
        if(nameOfIdField.isEmpty())
            return false;
        try{// Alanı mevcut sınıf içerisinde ara:
            Field fl = cls.getDeclaredField(nameOfIdField);
            return (fl != null);
        }
        catch(NoSuchFieldException | SecurityException exc){// Alan yoksa, üst sınıfa bak
            return checkFieldIsExist(cls.getSuperclass(), nameOfIdField);
        }
    }
    /**
     * Verilen sınıfın bir târih - saat, saat veyâ târih sınıfı olup, olmadığını
     * kontrol eder<br>
     * Şu dört sınıf târih saat sınıfıdır:<br>
     * - {@code java.time.LocalDate}<br>
     * - {@code java.time.LocalDateTime}<br>
     * - {@code java.time.LocalTime}<br>
     * - {@code java.util.Date}<br>
     * - {@code java.sql.Date}<br>
     * @param cls Denetlenmesi istenen sınıf
     * @return Hedef sınıf zamânla ilgiliyse {@code true}, aksi hâlde {@code false}
     */
    public boolean isAboutDateTime(Class<?> cls){
        if(cls == null)
            return false;
        if(cls.equals(LocalDate.class) || cls.equals(LocalDateTime.class) || 
            cls.equals(LocalTime.class) || cls.equals(Date.class) || cls.equals(java.sql.Date.class))
            return true;
        return false;
    }
    /**
     * Verilen sınıfın bir sayı sınıfı olup, olmadığını kontrol eder<br>
     * @param cls Hedef sınıf
     * @return Sınıf bir sayı sınıfıysa {@code true}, aksi hâlde {@code false}
     */
    public boolean isAboutNumber(Class<?> cls){
        if(cls == null)
            return false;
        if(cls.isPrimitive()){
            if(!cls.equals(char.class) && !cls.equals(boolean.class))
                return true;
            return false;
        }
        try{
            Class<?> asCasted = cls.asSubclass(Number.class);
            return asCasted != null;
        }
        catch(ClassCastException exc){
            return false;
        }
    }
    /**
     * Nesnenin, sınıfa çevrilmesinin şu kapsamda olup, olmadığı sorgulanır:<br>
     * - {@code List}'ten {@code Array}'e<br>
     * - {@code Array}'den {@code List}'e<br>
     * - {@code List}'den {@code List}'e<br>
     * - {@code Array}'den {@code Array}'e<br>
     * @param cls Hedef sınıf
     * @param obj Dönüştürülmek istenen nesne
     * @return Dönüşüm belirtilen kapsamdaysa {@code true},değilse {@code false}
     */
    public boolean isAboutListArrayConverting(Class<?> cls, Object obj){
        if(cls == null || obj == null)
            return false;
        boolean matched1st = false;
        boolean matched2nd = false;
        if(cls.isArray())
            matched1st = true;
        if(obj.getClass().isArray())
            matched2nd = true;
        if(!matched1st){
            try{
                Class<?> casted = cls.asSubclass(List.class);
                if(casted == null)
                    return false;
                matched1st = true;
            }
            catch(ClassCastException exc){
                return false;
            }
        }
        if(!matched2nd){
            try{
                Class<?> casted = obj.getClass().asSubclass(List.class);
                if(casted == null)
                    return false;
                matched2nd = true;
            }
            catch(ClassCastException exc){
                return false;
            }
        }
        return matched1st & matched2nd;
    }
    /**
     * Verilen nesnenin verilen koleksiyon içerisinde olup, olmadığına bakılır<br>
     * @param <T> Koleksiyonun "generic" sınıfını simgeleyen tip
     * @param list Verinin aranacağı koleksiyon
     * @param value Aranacak veri
     * @return Veri liste içerisinde ise {@code true}, aksi hâlde {@code false}
     */
    public <T> boolean isInTheList(Collection<T> list, T value){
        if(value == null || list == null)
            return false;
        for(T obj : list){
            if(obj.equals(value))
                return true;
        }
        return false;
    }

    // ARKAPLAN İŞLEM YÖNTEMLERİ:
    /**
     * Verilen özellik haritası {@code null} ise {@code null} döndürülür<br>
     * Verilen özellik haritası boş ise, yeni oluşturulan nesne döndürülür<br>
     * Verilen veriyi hedef sınıftaki nesneye zerk ederek nesne üretmeye çalışır
     * Şu an parametresiz yapıcı yöntemi bulunmayan sınıfın örneği üretilemiyor;
     * fakat nesnenin kullanıcı tarafından sağlanması destekleniyor
     * {@code enum} değerler için "getter" erişim yöntemi aranmıyor; yanî enum 
     * değerin gizli olmaması lazım<br>
     * {@code scanSuperClasses} parametresi sadece alanı tararken geçerlidir
     * @param <T> Sınıf örneği istenen sınıf
     * @param targetClass Örneği istenen sınıf
     * @param data Sınıfın örneğine zerk edilmesi istenen özellik değerleri
     * @param tryForceCasting Özelliğin veri tipi uyuşmadığında,
     * dönüşüm için ek yöntem uygulanmasını istiyorsanız {@code true} yapın<br>
     * @param codingStyle 'setter' yöntemine ihtiyaç duyulması
     * @param useGivenInstance Veriler verilen {@code instance} referansındaki
     * nesneye zerk edilecekse {@code true} olmalıdır.<br>
     * @param instance {@code useGivenInstance} {@code true} ise hedef nesne
     * durumunda bu yöntemin hangi kodlama standardına göre aranacağı bilgisi<br>
     * @param scanSuperClasses
     * @param forceAccessibility
     * @return Verilen verilerin zerk edildiği sınıf örneği veyâ {@code null}
     */
    private <T> T produceInjectedObject(Class<T> targetClass, Map<String, ? extends Object> data,
            CODING_STYLE codingStyle, boolean tryForceCasting
            /*, boolean isIncludeNoParameterConstructor, List<Object> parameterForConstructor*/,
            boolean useGivenInstance, T instance, boolean scanSuperClasses, boolean forceAccessibility){
        try{// Hedef veri tipinin uygunluğunu kontrol et
            if(isNotUserDefinedClass(targetClass))
                return null;
        }
        catch(IllegalArgumentException exc){
            System.err.println(exc.toString());
            return null;
        }
//        if(!isIncludeNoParameterConstructor(targetClass)){} : Eklenecek inşâAllâh
//          else
        T obj = null;
        if(useGivenInstance && instance != null)// Verilerin zerk edileceği nesne kullanıcı tarafından verildiyse ve 'null' değilse;
            obj = instance;
        else// Diğer durumda yeni bir sınıf örneği oluştur
            obj = produceInstance(targetClass);
        if(obj == null || data == null)// Hedef veri tipinin örneği oluşturulamadıysa veyâ verilen özellik haritası = null
            return null;
        if(data.isEmpty())// Verilen özellik haritasında bir özellik yoksa..
            return obj;
        if(targetClass.isEnum())
            return getEnumByData(targetClass, data.values().iterator().next());
        List<String> targets = new ArrayList<String>();
        targets.addAll(data.keySet());
        List<Field> fields = getFields(targetClass, targets, scanSuperClasses);// Bu sınıfta bulamazsan, üst sınıfı ara
        for(Field fl : fields){
            if(fl == null)
                continue;
            Class<?> clsField = fl.getType();
            String flName = fl.getName();
            Object value = data.get(flName);
            if(value == null){// Eğer veri 'null' ise, ilgili alan listede olabilir
                // olmayabilir; kesin sonuç için isim koleksiyonunu kontro et:
                if(!isInTheList(data.keySet(), flName))
                    continue;
                if(clsField.isPrimitive())// Temel veri tipine 'null' değer zerk edilemez
                    continue;
            }
            boolean execSetter = false;// "'setter' metodunu çalıştır" bayrağı
            boolean forceCast = false;// 'Veriyi çevirmeye zorla' bayrağı
            boolean isSuccessful = false;// 'İşlem başarılı oldu' bayrağı
            try{
                fl.set(obj, value);
                isSuccessful = true;
            }
            catch(IllegalArgumentException | SecurityException | ExceptionInInitializerError exc){
//                System.err.println("exc : " + exc);
                if(exc.getClass().equals(IllegalArgumentException.class))
                    if(tryForceCasting)
                        forceCast = true;
                execSetter = true;
            }
            catch(IllegalAccessException excFromIllegal){
                if(forceAccessibility){
                    try{
                        fl.setAccessible(true);
                        fl.set(obj, value);
                        isSuccessful = true;
                        fl.setAccessible(false);
                    }
                    catch(IllegalAccessException | IllegalArgumentException | SecurityException | ExceptionInInitializerError exc2nd){
                        execSetter = true;
                    }
                }
            }
            Method setterMethod = null;
            if(!isSuccessful && execSetter){
                setterMethod = getSpecialMethod(targetClass, flName, METHOD_TYPES.SET, codingStyle, scanSuperClasses);
                if(setterMethod != null){// 'setter' metodu varsa kullan:
                    Map<String, Object> resultOfInvoking = new HashMap<String, Object>();
                    invokeMethod(obj, setterMethod, new Object[]{value}, forceAccessibility, resultOfInvoking);
                    isSuccessful = (Boolean) resultOfInvoking.get("result");
                    if(!isSuccessful){// Metot çalıştırma başarısız olduysa;
                        if(tryForceCasting && value != null)// (veri 'null' ise zâten metot çalıştırma başarılı olurdu..)
                            forceCast = true;
                    }
                }
            }
            if(!isSuccessful && forceCast){// Veri tipini dönüştürmeye çalış:
                Object casted = getCastedObject(clsField, value);
                if(value != null && casted != null){// Veri hedef alanın tipine çevrilemedi (fakat metodun girdi tipine çevrilebilir)
                    try{
                        fl.set(obj, casted);
                        isSuccessful = true;
                    }
                    catch(IllegalArgumentException | SecurityException | ExceptionInInitializerError exc3rd){}
                    catch(IllegalAccessException exc3rdIllegal){
                        if(forceAccessibility){
                            try{
                                fl.setAccessible(true);
                                fl.set(obj, casted);
                                isSuccessful = true;
                                fl.setAccessible(false);
                            }
                            catch(IllegalAccessException | IllegalArgumentException | SecurityException | ExceptionInInitializerError exc2nd){}
                        }
                    }
                }
                if(!isSuccessful && execSetter){
                    if(setterMethod != null){// Alanın bir 'setter' metodu varsa;
                        Map<String, Object> resultOfInvoking = new HashMap<String, Object>();
                        invokeMethod(obj, setterMethod, new Object[]{casted}, forceAccessibility, resultOfInvoking);
                        isSuccessful = (Boolean) resultOfInvoking.get("result");
                        if(!isSuccessful){// Metot çalıştırma başarısız olduysa;
                            // Veriyi metodun girdi tipine çevirmeye çalış:
                            Class<?> inputType = setterMethod.getParameterTypes()[0];
                            if(inputType != null){
                                casted = getCastedObject(inputType, value);
                                if(casted != null){
                                    invokeMethod(obj, setterMethod, new Object[]{casted}, forceAccessibility, resultOfInvoking);
                                    isSuccessful = (Boolean) resultOfInvoking.get("result");
                                    if(!isSuccessful){
                                        System.out.println("Tüm çabalara rağmen veri hedef alana zerk edilemedi : " + fl.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return obj;
    }
    private <T> T getProducedInstanceForEnumMain(Class<T> targetEnum){
        if(targetEnum == null)
            return null;
        T obj = null;
        if(targetEnum.isEnum()){
            try{
                obj = targetEnum.getEnumConstants()[0];
            }
            catch(ArrayIndexOutOfBoundsException exc){
                System.err.println("exc... : " + exc.toString());
            }
        }
        return obj;
    }
    private String getParameterForConstructorOfWrapperBasicClass(Class<?> cls){
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
     * Verilen sınıfın kullanıcı sınıfı olup, olmadığıyla ilgili sorgulama yapar<br>
     * Şu durumlarda {@code true} döner:<br>
     * - Verilen sınıf temel veri tipi ise<br>
     * - Verilen sınıf temel veri tipi sarmalayıcısı ise<br>
     * - Verilen sınıf sık kullanılan koleksiyonlardan biri veyâ alt sınıfı ise<br>
     * - Verilen sınıf sık kullanılan zamân veri tiplerinden biriyse<br>
     * - Verilen sınıf {@code java.io.File}, {@code java.math.BigInteger} gibi 
     * özel amaçlı bir sınıf ise<br>
     * @param cls Kontrol edilmek istenen sınıf
     * @return Sınıf kullanıcı tanımlı bir sınıf değilse {@code true}, aksi hâlde {@code false}
     */
    public boolean isNotUserDefinedClass(Class<?> cls){
        if(cls == null)
            return true;
        String sourceClassName = cls.getName();
        for(String type : basicDataTypes){
            if(sourceClassName.equals(type))
                return true;
        }
        if(cls.isArray() || cls.isEnum())// Bir dizi veyâ sâbit değerli sınıf (enum) ise
            return true;
        switch(sourceClassName){// Bâzı özel dosya tipleri temel veri tipi sayılır
            case "java.io.File" : return true;
            case "java.time.LocalTime" : return true;
            case "java.time.LocalDateTime" : return true;
            case "java.time.LocalDate" : return true;
            case "java.util.Date" : return true;
            case "java.sql.Date" : return true;
            case "java.math.BigDecimal" : return true;
            case "java.math.BigInteger" : return true;
        }
        List<Class<?>> otherBasics = new ArrayList<Class<?>>();
        otherBasics.add(List.class);
        otherBasics.add(Map.class);
        otherBasics.add(Set.class);
        otherBasics.add(Properties.class);
        otherBasics.add(Number.class);
        for(Class<?> clsCollection : otherBasics){
            try{
                Class<?> castedClass = cls.asSubclass(clsCollection);
                if(castedClass != null)
                    return true;
            }
            catch(ClassCastException exc){}
        }
        return false;
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
    private List<Class<?>> getClassesOnTheRoot(File root, String fullNameFromRoot, boolean isAppRoot){
        if(root == null)
            return null;
        List<Class<?>> result = new ArrayList<Class<?>>();
        String path = null;
        if(!isAppRoot)
            path = (fullNameFromRoot == null ? root.getName() : fullNameFromRoot + "." + root.getName());
        for(File fl : root.listFiles()){
            if(fl == null)
                continue;
            if(fl.isFile()){
                if(!fl.getName().endsWith(".class"))// Sadece '.class' uzantılı dosyaları yüklemeye çalış
                    continue;
                Class<?> cls = null;
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
                List<Class<?>> subList = getClassesOnTheRoot(fl, path, false);
                if(subList != null){
                    for(Class<?> cls : subList){
                        if(cls != null)
                            result.add(cls);
                    }
                }
            }
        }
        return result;
    }
    private <T extends Annotation> List<String> getAnnotatedFieldsByGivenAnnotation(Class<?> target, Class<T> annotation, boolean stopOnFirst){
        if(annotation == null || target == null)
            return null;
        List<String> values = new ArrayList<String>();
        try{
            Field[] fields = target.getDeclaredFields();
            for(Field fl : fields){
                boolean found = false;
                T[] specs = fl.getDeclaredAnnotationsByType(annotation);
                if(specs.length > 0){
                    for(T ann : specs){
                        if(ann != null){
                            values.add(fl.getName());
                            found = true;
                            break;
                        }
                    }
                }
                if(found && stopOnFirst)
                    break;
            }
        }
        catch(SecurityException exc){
            System.err.println("Bildirim (annotation) için alanlara bakılırken hatâ alındı : " + exc.toString());
            return null;
        }
        return values;
    }
    private void getFieldsMain(Class<?> cls, Map<String, Field> map, boolean takeFieldsInSuperClasses){
        if(cls == null)
            return;
        try{
            Field[] fields = cls.getDeclaredFields();
            for(Field fl : fields){
                // Üst sınıftaki alanların alt sınıftakilerin yerine geçmemesi
                // için şu şart eklenmelidir:
                if(map.get(fl.getName()) == null)
                    map.put(fl.getName(), fl);
            }
            if(takeFieldsInSuperClasses){
                Class<?> clsSuper = cls.getSuperclass();
                if(clsSuper != null){
                    if(!clsSuper.equals(Object.class)){
                        getFieldsMain(clsSuper, map, takeFieldsInSuperClasses);
                    }
                }
            }
        }
        catch(SecurityException exc){
            System.err.println("exc : " + exc.toString());
        }
    }
    /**
     * Verilen sınıfın tüm metotlarını alır ve döndürür<br>
     * Üst sınıfların taranması {@code scanSuperClasses} parametresine bağlıdır<br>
     * @param cls Hedef sınıf
     * @param map Metotların eklendiği harita (özyinelemeli döngü için)
     * @param scanSuperClasses Üst sınıflar taranacak mı
     */
    private void getMethodsMain(Class<?> cls, Map<String, Method> map,
        boolean scanSuperClasses){
        if(cls == null)
            return;
        try{
            Method[] methods = cls.getDeclaredMethods();
            for(Method m : methods){
                // Üst sınıftaki metotların alt sınıftakilerin yerine geçmemesi
                // için şu şart eklenmelidir:
                if(map.get(m.getName()) == null)
                    map.put(m.getName(), m);
            }
            if(scanSuperClasses){
                Class<?> clsSuper = cls.getSuperclass();
                if(clsSuper != null){
                    if(!clsSuper.equals(Object.class)){
                        getMethodsMain(clsSuper, map, scanSuperClasses);
                    }
                }
            }
        }
        catch(SecurityException exc){
            System.err.println("exc : " + exc.toString());
        }
    }

// ERİŞİM YÖNTEMLERİ:
    //ANA ERİŞİM YÖNTEMİ
    /**
     * {@code Reflector} nesnesini "singleton" olarak döndürür
     * @return {@code "Reflector"} servisi
     */
    public static Reflector getService(){
        if(serv == null)
            serv = new Reflector();
        return serv;
    }
    // GİZLİ ERİŞİM YÖNTEMLERİ:
    private HashMap<Class<?>, Class<?>> getMapOfPrimitiveToWrapper(){
        if(mapOfPrimitiveToWrapper == null){
            mapOfPrimitiveToWrapper = new HashMap<Class<?>, Class<?>>();
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
    /**
     * Temel veri tiplerinin ve sarmalayıcılarının tam isimlerini içeren yeni
     * bir liste nesnesi döndürür<br>
     * @return Temel veri tiplerini içeren {@code List} nesnesi
     */
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
        
//        li.add("java.time.LocalDate");
//        li.add("java.time.LocalDateTime");
//        li.add("java.time.LocalTime");
//        li.add("java.util.Date");
//        li.add("java.sql.Date");
//        li.add("java.lang.Number");
//        li.add("java.math.BigDecimal");
//        
//        li.add("java.io.File");
//        
//        li.add("java.util.List");
        return li;
    }
    /**
     * @return SQL ve ISO formatı için derlenmiş {@code DateTimeFormatter}
     */
    protected DateTimeFormatter getSQLAndISODFormatter(){
        if(sqlAndIsoDFormatter == null)
            sqlAndIsoDFormatter = new DateTimeFormatterBuilder().
                parseCaseInsensitive().appendValue(ChronoField.YEAR, 4)
                .optionalStart().appendLiteral('-').optionalEnd()
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .optionalStart().appendLiteral('-').optionalEnd()
                .appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter();
        return sqlAndIsoDFormatter;
    }
    protected DateTimeFormatter getSQLAndISODTFormatter(){
        if(sqlAndIsoDTFormatter == null)
            sqlAndIsoDTFormatter = new DateTimeFormatterBuilder()
                .optionalStart().append(getSQLAndISODFormatter()).optionalEnd()
                .optionalStart().appendLiteral(' ').optionalEnd()
                .optionalStart().appendLiteral('T').optionalEnd()
                .optionalStart().append(DateTimeFormatter.ISO_TIME).optionalEnd()
                .toFormatter();
        return sqlAndIsoDTFormatter;
    }
}