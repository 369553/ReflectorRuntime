# ReflectorRuntime v3.1.0

## ReflectorRuntime nedir, ne işe yarar?

- Tek cümleyle söylemek istersek;
  
  Düşük ve yüksek seviyeli nesne manipülasyonunu konforlu hâle getiren kitâplık
  diyebilirim.

- Kitâplık Java'da nesne manipülasyonlarını kapsayıcı, esnek ve dayanıklı biçimde destekleyen metotlar içermektedir.

- Kitâplığın en temel hedefleri şunlardır:
  
  1. Veri dönüştürme işlemlerini daha esnek hâle getirme,
  
  2. Nesnelerden alan verilerini kolayca toplama,
  
  3. Verileri nesnelere zerk etme (injection)

- Bu kullanma kılavuzu boyunca ayrıntıların örtüldüğü, kullanımı daha konforlu ve kolay olan işlemler yüksek seviyeli, daha fazla ayar ve parametrenin yapılabildiği, Java reflection API'nin daha kullanışlı hâle getirildiği işlemler ise düşük seviyeli olarak nitelendirilmektedir.

- Yazar : Mehmet Akif SOLAK

## Kitâplık Ne Tür Esneklikler Sağlıyor, İşte Birkaçı

#### 1) Verileri Aktarılmış (Zerk Edilmiş, 'Injected') Nesne Üretimi

- Bir nesneyi ve o nesneye âit alan ('field') değerlerini sağladığınızda verilen özellikler nesneye zerk edilir. Bu, çok zor bir işlem değildir; fakat bu kitâplığın farkı bu sorunu minimize edecek şekilde esnek dönüşümler ve farklı usûller sağlamasıdır.

- Bu esneklik ve dayanıklılıklardan bir kısmı şu şekildedir:
  
  - Hedef alana erişim izni yoksa, seçtiğiniz kodlama yöntemiyle (`Reflector.CODING_STYLE`) hedef alan için 'setter' yöntemini bulma ve verileri zerk etme
  
  - Hedef alan için üst sınıfları tarama
  
  - Hedef alan veyâ metot için erişim zorlaması yaparak veriyi nesneye zerk etme
  
  - Veriler arasındaki basit uyuşmazlıkları 'type casting' ile ortadan kaldırma
  
  - 'Type casting' ile ortadan kalkmayan veriler için detaylı analizler yaparak verileri hedef alanın veri tipine dönüştürme:
    
    - SQL ve ISO formatlarındaki târih - saat verilerini tanıma
    
    - Java tarafından dönüşüm sağlanmayan dizi ve çok boyutlu dizi tipleri için dönüşüm sağlama
      (misal, `Integer[][]` -> `int[][]` veyâ `int[][]` -> `double[][]`)
    
    - `List<T>` gibi koleksiyonları diziye, dizileri `List` gibi koleksiyonlara çevirme; bunu yaparken boyut -> derinlik dönüşümü yapma;
      misal, `List<List<Integer>>` -> `int[][]` gibi bir dönüşüm yapılabilir.
      İstenildiği takdirde, dönüşümü sağlanamayan elemanlar görmezden gelinebilir. Bu, karışık tipli dizilerden verilen tipe uygun olan elemanları seçilebilmesi için de kullanılabilir.
    
    - Metîn olarak verilen `Enum` verilerinin karşılığı olan `Enum` değişkeni bulma
    
    - Java tarafında uygulanması zor olan generic sınıf nesnelerinin generic tiplerinin göz önüne alınarak uyumsuz verinin her derinlikte hedef generic tipe çevrilerek nesne tipi dönüştürülmesi.
  
  - İşlemleri düşük seviyede yapmak için yapılandırma parametrelerinin sunulması

#### 2) Esnek Veri Dönüşümü

- Yukarıda zikredilen esneklikler bir değişkenin veri dönüşümünde de kullanılabilir.

#### 3) Nesnenin Alan Değerlerinin Alınması

- Bir nesnenin verilen alan değerleri bir metotla kolayca alınabilir; alanlara alan üzerinden veyâ metot üzerinden verilen yapılandırmaya göre erişim sağlanabilir.

#### 4) Dosya Yolundan Sınıf Yükleme

- Verilen dosya yolunun altındaki tüm sınıflar çalışma zamânında uygulamaya yüklenebilir.

#### 5) Derinlik ve Boyut Ölçümü

- `List<T>` derinliklerini ve dizilerin boyutlarını öğrenmekte yardımcı olabilir.

#### 6) Kolay Nesne ve Dizi Üretimi

- Verilen veri tipinde bir nesnenin veyâ verilen veri tipinde ve verilen boyutta bir dizinin üretilmesi için yardımcı fonksiyon barındırır.

## KULLANIMI

- Detaylı kullanım için el kitâbına bakabilirsiniz; burada temel özellikler ve kullanım anlatılmaktadır.

#### 1) JAR Dosyası Olarak Hâzırlama

- Projeyi klonlayın:
  
  ```bash
  git clone https://github.com/369553/ReflectorRuntime.git
  ```

- Ardından *ReflectorRuntime/src* dizinine gidin ve komut satırını / uç birim (terminal)i o dizinde açın.

- Derleme ve paketleme komutlarını çalıştırın:
  
  - Windows kullanıyorsanız şu komutu çalıştın:
    `mkdir ReflectorOutput & javac -encoding UTF-8 -d ReflectorOutput ReflectorRuntime/*.java -parameters & cd ReflectorOutput & jar cf ReflectorRuntime.jar ReflectorRuntime/*.class`
  
  - Linux kullanıyorsanız şu komutu çalıştırın:
    `mkdir ReflectorOutput && javac -encoding UTF-8 -d ReflectorOutput ReflectorRuntime/*.java -parameters && cd ReflectorOutput && jar cf ReflectorRuntime.jar ReflectorRuntime/*.class`

- JAR dosyası ReflectorOutput dizini içerisinde oluşmuş olmalı.

- Kullanıcı el kitâbı oluşturmak için *ReflectorRuntime/src* dizini içerisinde komut satırında / uç birim (terminal)de şu komutu çalıştırabilirsiniz:
  `javadoc -encoding UTF-8 -d docReflector -charset UTF-8 ReflectorRuntime/Reflector.java`

#### 2) Servise Erişim

- Sınıf hizmetlerine erişmek için sınıfın müşahhas bir örneğini (her defasında aynı nesne) döndüren `Reflector.getService()` statik fonksiyonunu kullanabilirsiniz.
- Bunun yerine yeni bir `Reflector` nesnesi de oluşturabilirsiniz.
- `Reflector.getService()` ile gelen statik nesne bir küresel değişkeni manipüle etmediğinden "thread-safe" olarak değerlendirilebilir.

#### 3) Kullanım : Metotlar, İşlevler

- Bu bölümde en çok kullanlan bâzı işlevlerin nasıl kullanıldığına, ardından düşük seviye işlemler için sağlanan metotlara değinilecektir inşâAllâh..

##### 3.1) Alan Verilerini Alma

- Alan verileri, `getValueOfFields()` metotlarıyla alınabilir:
  
  ```java
  User u = new User();
  Reflector serv = Reflector.getService();
  Map<String, Object> data = serv.getValueOfFields(user, null,
                          CODING_STYLE.CAMEL_CASE, true, true);
  data.entrySet().forEach(System.out::println);
  ```

- Bu metotta alan isimlerine `null` verdiğinizde tüm alanlar alınır; fakat alan isim listesi yerine `Field[]` tipinde bir parametre bekleyen diğer metotta bu geçerli değildir.

- Alanlar bu sınıfın veyâ bu sınıfın üst sınıflarının alanları olabilir. Bu durumda `java.lang.Object` dâhil olmamak üzere `java.lang.Object`'e kadar üst sınıfların taranmasını metodun `scanSuperClasses` parametresiyle belirtebilirsiniz.

- Alanlar erişim belirteciyle izole edilmiş olabilir; bu durumda `Reflector` alan için bir "**getter**" metodu arayıp, çalıştırır.

- Eğer alana veyâ "**getter**" metoduna zorla erişmek isterseniz `forceAccessibility` parametresine `true` veriniz. Bu, `private` alana ve `private` metoda erişim sağlayabilir. Kullandığınız güvenlik yöneticisi (`SecurityManager`) buna izin veriyor olabilir.

- Çoğu kez alanlar dış dünyâdan izole edilirler (`private` belirteciyle). Bu durumda ilgili alanın "**getter**" metodunu arayarak alanın verisini almak için bu metodun nasıl yazıldığını bilmemiz lazımdır. Bu sebeple `CODING_STYLE codingStyle` parametresiyle kodlama biçimi belirtmelisiniz.

##### 3.2) Veri Dönüşümü

- `ReflectorRuntime`'daki veri dönüşümleri aslında zerk (enjeksiyon) işlemlerini de içeren, kapsamlı veri dönüşümleridir.

- Bunun için pek çok alt işlem desteklenmektedir, bu işlemleri veyâ bu işlemlerin neredeyse hepsini tek metot  (`getCastedObject()`) altında kullanabilirsiniz.

- Bu bölümdeki anlatımlarda `Reflector()` nesnesi `ref` değişkeniyle ifâde edilmektedir. `ref = new Reflector();`

###### 3.2.1) Metîn verilerinden temel veri tiplerine dönüşüm

- Metîn verilerinden temel veri tiplerine, sarmalayıcı veri tiplerine, genişletilmiş veri tiplerine (`BigDecimal`, `BigInteger` gibi) dönüşüm yapılabilir:
  
  ```java
  String strBool = "TrUe";
  boolean value = ref.getCastedObject(boolean.class, strBool);
  System.out.println("value : " + value);// value : true
  
  String strInt = "233.0";
  int valInt = ref.getCastedObject(int.class, strInt);
  System.out.println("valInt : " + valInt);// valInt : 233
  ```

###### 3.2.2) Sayı tipleri arasında dönüşüm

- Sayı tipleri arasında dönüşüm yapabilirsiniz:
  
  ```java
  // Sayısal verileri birbirleri arasında çevirin:
  int value = 153;
  BigInteger bigInt = ref.getCastedObject(BigInteger.class, value);
  System.out.println("bigInt : " + bigInt);// 153
  
  Long l = ref.getCastedObject(Long.class, BigDecimal.ONE);
  System.out.println("Long değeri : " + l);// 1
  ```

###### 3.2.3) Metin ve tarih - saat tipleri arasındaki dönüşümler

- SQL ve ISO biçimindeki metînlerden târih saat veri tiplerine veyâ târih saat tiplerinden metînlere dönüşüm yapabilirsiniz:
  
  ```java
  String strDate = "2025-05-12 12:34:32";
  LocalDateTime date = ref.getCastedObject(LocalDateTime.class, strDate);
  System.out.println("Date : " + date);// Date : 2025-05-12T12:34:32
  
  // Târih saat nesnesini SQL veyâ ISO formatında metîn olarak alın:
  String strSQLDate = ref.getDateTimeTextAsSQLStyle(date);
  System.out.println(strSQLDate);// strSQLDate : 2025-05-12 12:34:32
  ```

###### 3.2.4) Diziler arasındaki dönüşümler

- Temel veyâ sarmalayıcı tiplerin dizi ve matrisleri arasında dönüşümler yapılabilir:
  
  ```java
  int[][] src = new int[][]{{14, 6, 1}, {12, 63, 33}};
  double[][] arr = ref.getCastedObject(double[][].class, src);
  for(int sayac = 0; sayac < arr.length; sayac++){
      for(int s2 = 0; s2 < arr[sayac].length; s2++){
          System.out.print("arr[" + sayac + "][" + s2 + "] = "
                                                + arr[sayac][s2] + " ");
      }
  }
  // [0][0] = 14.0    [0][1] = 6.0    [0][2] = 1.0    
  // [1][0] = 12.0    [1][1] = 63.0    [1][2] = 33.0    
  ```

- İnanılmaz şekilde, dizi verilerini `List` sınıfına, `List` verilerini dizilere çevirebilirsiniz. `Reflector` veriler arasındaki derinlik uyumunu araştırır:
  
  ```java
  int[][] src = new int[][]{{14, 6, 1}, {12, 63, 33}};
  List<Integer> list = ref.getCastedObject(List.class, src);
  System.out.println("list : " + list);
  // list : [[14, 6, 1], [12, 63, 33]]
  
  // List verisinden diziye çevrim:
  List<Double> liDouble = new ArrayList<Double>();
  liDouble.add(34.3);
  liDouble.add(23.1);
  liDouble.add(8.11);
  float[] arr = ref.getCastedObject(float[].class, liDouble);
  for(int sayac = 0; sayac < arr.length; sayac++){
      System.out.println("[" + sayac + "] = " + arr[sayac] + "\t");
  }// [0] = 34.3    [1] = 23.1    [2] = 8.11    
  ```

###### 3.2.5) Metîn - enum dönüşümü

- Metînlerden enum sınıfındaki nesne alınabilir:
  
  ```java
  Reflector.CODING_STYLE codingStyle =
      ref.getCastedObject(Reflector.CODING_STYLE.class, "CAMEL_CASE");
  System.out.println("style : " + codingStyle);// style : CAMEL_CASE
  ```

###### 3.2.6) Metîn - File, UUID sınıflarına dönüşümü

- Metîn verilerinden bu nesnelerin üretimi de sağlanmaktadır:
  
  ```java
  String path = System.getProperty("java.io.tmpdir");
  File fl = ref.getCastedObject(File.class, path);
  
  String strUuid = "fbc1437a-c931-4ffa-af48-99242290feae";
  UUID id = ref.getCastedObject(UUID.class, strUuid);
  System.out.println(id);// fbc1437a-c931-4ffa-af48-99242290feae
  ```

###### 3.2.7) Sınıf isimlerinden sınıfların yüklenmesi

- Sınıf isimleriinden sınıfların yüklenmesi işlemi de `getCastedObject()` kısayol metoduyla yapılabilir:
  
  ```java
  String strClassName = "java.lang.Object";
  Class<?> cls = ref.getCastedObject(Class.class, strClassName);
  System.out.println("cls : " + cls);// cls : class java.lang.Object
  ```

> ***NOT :*** `getCastedObject()` ile yapılan işlem bâzı noktalarda hedef veri tipine hangi alt fonksiyonun çalıştırılmasının tespitidir. Yukarıdaki gösterilen işlemler için ayrı isimlerde metotlar bulunmaktadır. Sırf belli bir amaca mâtuf işlem yapılacağı durumlarda o alt metotlar kullanılabilir.

##### 3.3) Alanları alma

- Alan değerlerini `java.lang.reflect` kitâplığı altındaki `Field` nesnesi olarak alabilirsiniz. Bu, Java Reflection kullanımını kolaylaştırmaktadır. Tek parametreyle üst sınıfların taranıp, taranmayacağını belirtmek, isim listesi olarak verilen alanların `Field` listesi olarak alınması gibi kolaylıklar sunulmaktadır:
  
  ```java
  List<Field> liFields = ref.getFields(Author.class, true);
  ```

- Dilenirse ikinci parametre 'false' verilerek sadece verilen sınıf taranabilir.

- İstenilirse, sadece ismi verilen alanlar alınabilir:
  
  ```java
  List<String> names = new ArrayList<String>();
  names.add("book");names.add("id");names.add("age");
  List<Field> liFields = ref.getFields(Author.class, names, true);
  ```

##### 3.4) Mevcut nesnelere veri zerki (enjeksiyonu, injection) ve mevcut olmayan nesneler için üretim

- Bu maksatla kullanılan yüksek seviyeli metot `injectData()` metodudur. Bu metot sırasıyla şu parametreleri alır:
  
  - `T targetObject` : Verinin zerk edilmesi istenen nesne
  
  - `Map<String, ? extends Object> data` : Anahtarı alan ismi, değeri zerk edilmek istenen alan değeri olan veri haritası
  
  - `CODING_STYLE codingStyle` : Sınıfın kodlanma biçimi. Bu, "setter" metotlarının isimlerinin çıkartılması için alınır.  

-  Kullanım örneği:
  
  ```java
  Map<String, Object> fieldValues =
                      new HashMap<String, Object>();
  User u = new User("Mehmet Âkif");
  fieldValues.put("origin", "Turkish");
  List<String> li = new ArrayList<String>();
  li.add("Back-end engineer");
  li.add("Database developer");
  li.add("AI and NLP developer");
  li.add("Java developer");
  li.add("Algorithm developer");
  fieldValues.put("focuses", li);
  User u = new User();
  ref.injectData(u, fieldValues, CODING_STYLE.CAMEL_CASE);
  
  System.out.println("u.origin : " + u.origin);// u.origin: Turkish
  ```

##### 3.5) Nesne, Dizi ve Liste üretimi

###### 3.5.1) Nesne örnekleri oluşturma

- İsterseniz veri zerk edilerek, isterseniz de veri zerk edilmeden nesne örnekleri oluşturabilirsiniz.

- Kullanıcı tanımlı sınıfların nesne örneklerinin oluşturulması için bir parametresiz yapıcı metoda ihtiyaçları vardır.

- Bunun için `produceInstance()` ve `produceInjectedObject()` metotlarını kullanabilirsiniz. Birincisi veri zerk edilmeden bir sınıf örneği oluşturmak için, ikincisi ise veri zerk edilerek sınıf örneği (nesne) oluşturmak için kullanılır:
  
  ```java
  User u = ref.produceInstance(User.class);
  
  // Veri zerk edilerek nesne oluşturma:
  Map<String, Object> fieldValues =
                      new HashMap<String, Object>();
  
  fieldValues.put("id", UUID.randomUUID());
  fieldValues.put("name", "Mehmet Âkif");
  fieldValues.put("origin", "Turkish");
  List<String> li = new ArrayList<String>();
  li.add("Back-end engineer");
  li.add("Database developer");
  li.add("AI and NLP developer");
  li.add("Java developer");
  li.add("Algorithm developer");
  fieldValues.put("focuses", li);
  
  User u = ref.produceInjectedObject(User.class,
                                      data, CODING_STYLE.CAMEL_CASE);
  System.out.println("u.name : " + u.name);// u.name : Mehmet Âkif
  ```

- ..

###### 3.5.2) Dizi örnekleri oluşturma

- Dizi ve matris örnekleri oluşturmak için birden fazla metot bulunmaktadır:
  
  ```java
  // Object temelli bir tipte dizi oluşturma:
  Integer[] arr = ref.produceArray(Integer[].class, 5);
  System.out.println("arr.length : " + arr.length);
  
  // Temel (primitive) tipte bir dizi oluşturma:
  int[][] matrix = ref.produceArrayReturnAsObject(int[][].class, 4);
  System.out.println("matrix.length : " + matrix.length);
  
  // 
  ```

- .

> ***NOT :*** Temel veri tipleri için `produceArrayReturnAsObject()` metodunun kullanılmasının sebebi `int[].class` sınıfının üst sınıfının `Object` olmamasından dolayı `T`'ye çevrilememesidir.

- .

- .

- .

- .

- ..
