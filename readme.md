# ReflectorRuntime v3.0.0

## ReflectorRuntime nedir, ne işe yarar?

- Tek cümleyle söylemek istersek;
  
  Düşük ve yüksek seviyeli nesne manipülasyonunu konforlu hâle getiren kitâplık
  diyebilirim.

- Kitâplık Java'da nesne manipülasyonlarını kapsayıcı, esnek ve dayanıklı biçimde destekleyen metotlar içermektedir.

- Kitâplığın en temel hedefleri şunlardır:
  
  1. Veri dönüştürme işlemlerini daha esnek hâle getirme,
  
  2. Nesnelerden alan verilerini kolayca toplama,
  
  3. Verileri nesnelere zerk etme (injection)

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

#### 3) Metotlar, İşlevler

- En temel kullanım çeşitleri için bâzı örnekler : HENÜZ EKLENMEDİ...

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

- ... (devâm edilecek inşâAllâh)
