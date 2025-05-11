## ReflectorRuntime nedir, ne işe yarar?

- Bu kitâplık, Java'da çalışma zamânında verilerin nesnelere zerk (enjekte) edilmesi, nesneler üzerindeki değişikliklerin esnek ve kolay hâle gelmesi, nesne türetimi ve uyumluluk konularında yardımcı fonksiyonları içermektedir.

- En temelde birkaç ana hedef için bu kitâplık yazılmıştır:
  
  1. Nesne üretimi
  
  2. Verileri nesneye zerk etme
  
  3. Yüksek seviyede esnek dönüşüm sağlayan yapılarla veriler arasındaki uyumsuzlukları gidererek veri tipi dönüşümü sağlama

- Yazar : Mehmet Akif SOLAK

## Kitâplık Ne Tür Esneklikler Sağlıyor

#### 1) Verileri Aktarılmış (Zerk Edilmiş, 'Injected') Nesne Üretimi

- Bir nesneyi ve o nesneye âit alan ('field') değerlerini sağladığınızda verilen özellikler nesneye zerk edilir. Bu, çok zor bir işlem değildir; fakat bu kitâplığın farkı bu sorunu minimize edecek şekilde esnek dönüşümler ve farklı usûller sağlamasıdır.

- Bu esneklik ve dayanıklılıklardan bir kısmı şu şekildedir:
  
  - Hedef alana erişim izni yoksa, seçtiğiniz kodlama yöntemiyle (`Reflector.CODING_STYLE`) hedef alan için 'setter' yöntemini bulma ve verileri zerk etme
  
  - Veriler arasındaki basit uyuşmazlıkları 'type casting' ile ortadan kaldırma
  
  - 'Type casting' ile ortadan kalkmayan veriler için detaylı analizler yaparak verileri hedef alanın veri tipine dönüştürme:
    
    - SQL ve ISO formatlarındaki târih - saat verilerini tanıma
    
    - Java tarafından dönüşüm sağlanmayan dizi ve çok boyutlu dizi tipleri için dönüşüm sağlama
      (misal, `Integer[][]` -> `int[][]` veyâ `int[][]` -> `double[][]`)
    
    - `List<T>` gibi koleksiyonları diziye, dizileri `List<T>` gibi koleksiyonlara çevirme; bunu yaparken boyut -> derinlik dönüşümü yapma;
      misal, `List<List<Integer>>` -> `int[][]` gibi bir dönüşüm yapılabilir.
      İstenildiği takdirde, dönüşümü sağlanamayan elemanlar görmezden gelinebilir. Bu, karışık tipli dizilerden verilen tipe uygun olan elemanları seçilebilmesi için de kullanılabilir.
    
    - Metîn olarak verilen `Enum` verilerinin karşılığı olan `Enum` değişkeni bulma 

#### 2) Esnek Veri Dönüşümü

- Yukarıda zikredilen esneklikler bir değişkenin veri dönüşümünde de kullanılabilir

#### 3) Nesnenin Alan Değerlerinin Alınması

- Bir nesnenin verilen alan değerleri bir metodla kolayca alınabilir; alanların erişilebilir olması veyâ erişilebilir bir 'getter' yönteminin olması kâfîdir

#### 4) Dosya Yolundan Sınıf Yükleme

- Verilen dosya yolunun altındaki tüm sınıflar çalışma zamânında uygulamaya yüklenebilir.

#### 5) Derinlik ve Boyut Ölçümü

- `List<T>` derinliklerini ve dizilerin boyutlarını öğrenmekte yardımcı olabilir.

#### 6) Otomatik Veri Tipi Dönüşümü Analizi

- Verilen iki veri tipinin birbirine otomatik olarak 

#### 7) Kolay Nesne ve Dizi Üretimi

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

- Sınıf hizmetlerine erişmek için sınıfın müşahhas bir örneğini (her defasında aynı nesne) döndüren `Reflector.getService()` statik fonksiyonunu kullanın.

#### 3) Metodlar, İşlevler

- Uzun olmaması için parametreler burada tek tek yazılmamıştır; kod içerisindeki kılavuz belgeler (dökümantasyon) detayları barındırmaktadır.

- `produceInstance(...)` : Verilen sınıfın örneğini (nesnesini) oluşturur ve döndürür

- `injectData(...)` : Verilen verileri, verilen nesneye zerk eder.

- `checkAndConvertListAndArray(...)` : Verilen derinlik bağımsız `List<T>` nesnesini, boyutu eşleşecek şekilde hedef veri tipindeki diziye çevirir; boyut bağımsız diziyi, derinliği, boyutu yansıtacak şekilde hedef tipteki listeye çevirir.

- `findConstructorForNoParameter(...)` : Verilen sınıfın parametresiz yapıcı yöntemini -eğer varsa ve erişilebilir ise- döndürür.

- `getCastedObject(...)` : Verilen veriyi, verilen hedef sınıfa çevirmeye çalışır. Bunun için birden fazla yöntem denenir.

- `getCastedObjectFromString(...)` : Metîn olarak verilen veriyi, verilen hedef sınıf nesnesine çevirmeye çalışır.

- `getClassesOnTheAppPath()` : Uygulama kök dizini altındaki tüm sınıfları yükleyip, döndürür.

- `getClassesOnThePath(...)` : Verilen dizin altındaki sınıfları bulup, yükler, döndürür.

- `getDateObjectFromString(...)` : ISO veyâ SQL formatındaki târih - saat, târih ve saat metînlerini verilen hedef sınıfa çevirir.

- `getDateTimeTextAsSQLStyle(...)` : Verilen zamân verisini, SQL ile veri eklenirken kabûl edilen biçimdeki metne çevirir.

- `getDimensionOfArray(...)` : Verilen dizinin boyut bilgisini döndürür.

- `getDimensionOfList(...)` : Verilen listenin elemanlarını tarayarak listenin derînliğini araştırır, eğer dizi elemanı bir liste ise, derinlik bir arttırılır ve o da taranır

- `getEnumByData(..)` : Metîn biçimindeki `Enum` verisini hedef `Enum` sınıfının nesnesi olarak döndürür.

- `getFieldValuesBasicly(...)` : Verilen sınıfın özelliklerini toplar ve `Map` biçiminde döndürür.

- `getMethodNameDependsCodeStyle(...)` : İsmi verilen bir özelliğin ('attribute') 'getter' veyâ 'setter' yöntem ismini döndürür

- `getPrimitiveClassFromWrapper(...)` Verilen sarmalayıcı sınıfın temel sınıfını döndürür.

- `getProducedInstanceForEnum(...)` : Verilen `Enum` sınıfından bir değeri nesne olarak döndürür.

- `getSpecifiedFields(...)` : Verilen sınıfın yalnızca ismi verilen alanlarını `Field` nesnelerinden oluşan liste biçiminde döndürür.

- `getValueOfFields(...)` : Verilen nesnenin ismi veyâ alan nesnesinin kendisi olarak verilen alanlarının değerlerini `Map` biçiminde döndürür.

- `getWrapperClassFromPrimitiveClass(...)` : Verilen temel sınıfın sarmalayıcı sınıfını döndürür.

- `isPairingAutomatically(...)` : Verilen iki sınıfın herhangi bir casting işlemi olmaksızın birbirine otomatik olarak dönüşüp, dönüşmediğini denetler. Java otomatik sarmalama özelliğiyle sarmalanan sınıf ile temel hâli eşleşir.

- `isWrapperClassOfBasic` : Verilen sınıfın temel veri tipi sınıfının sarmalayıcısı olup, olmadığı sorgulanır.

- `produceArray(...)` : Verilen sınıfta, verilen uzunlukta dizi oluşturulur.

- `produceInjectedArray(...)` : Verilen hedef sınıf tipinde verilen verilerin zerk edildiği bir boyut bağımsız dizi döndürür.

- `produceInjectedList(...)` : Verilen dizi verisinden hareketle, uygun derinlikli, verileri aktarılmış ve hedef sınıf tipinde olan bir `List<T>` nesnesi oluşturulur.

- `produceInjectedObject(...)` : Verilen nesneye verilen alan özelliklerini zerk eder.

- 
