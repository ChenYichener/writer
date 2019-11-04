# 实现自定义Spring Boot starter

## 分析

在我们使用Spring Boot的时候，发现它是一个开箱即用的框架，这也是我们常说Spring Boot的一个非常大的特点，**自动配置**



### 分析注解

```java
@SpringBootApplication
	->
        @SpringBootConfiguration
        @EnableAutoConfiguration
        	->
        	@AutoConfigurationPackage
        	@Import({AutoConfigurationImportSelector.class})
        @ComponentScan
```

**Import**

使用`@Import`注解，可以将一个Bean快速的加入到IOC容器中，例如`AutoConfigurationImportSelector`类加入到了IOC容器中

**AutoConfigurationImportSelector**

```
private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
        MultiValueMap<String, String> result = (MultiValueMap)cache.get(classLoader);
        if (result != null) {
            return result;
        } else {
            try {
            	// 读取spring.factories文件
                Enumeration<URL> urls = classLoader != null ? classLoader.getResources("META-INF/spring.factories") : ClassLoader.getSystemResources("META-INF/spring.factories");
                LinkedMultiValueMap result = new LinkedMultiValueMap();

                while(urls.hasMoreElements()) {
                    URL url = (URL)urls.nextElement();
                    UrlResource resource = new UrlResource(url);
                    Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                    Iterator var6 = properties.entrySet().iterator();
                    
	// ................省略
}
```



阅读AutoConfigurationImportSelector的源码，我们发现Spring会在我们的classpath下面找寻**META-INF/spring.factories**这样的文件，并且会把它作为properties文件加载到程序的Properties集合中。

那么我们不妨阅读以下Druid的场景启动器，看看它的META-INF/spring.factories文件中写了什么

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure
```



读到这里我们明白了，原来Druid的starter自动配置的入口类是DruidDataSourceAutoConfigure，那么不妨我们自己也可以来模仿一个



# 步骤

### META-INF/spring.factories

首先在类路径下面创建META-INF/spring.factories文件

```properties
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=info.chenyichen.spring.boot.customstartsample01.HelloAutoConfiguration
```



### HelloAutoConfiguration

```java
@Configuration
@EnableConfigurationProperties(HelloAutoConfigrationProperties.class)
@ConditionalOnClass(HelloService.class)
public class HelloAutoConfiguration {

    @Autowired
    HelloAutoConfigrationProperties config;

    @Bean
    public HelloService helloService() {
        HelloService hs = new HelloService();
        hs.setGender(config.getGender());
        hs.setName(config.getName());
        return hs;
    }
}
```



**@ConditionalOnClass**表示当Spring IOC存在HelloService这个Bean时，这个自动配置类才会生效

**@EnableConfigurationProperties**表示让HelloAutoConfigurationProperties进行加载

**@Configuration**表示这是一个配置类，类似Spring的ApplicationContext.xml文件



### HelloAutoConfigurationProperties

```java
@ConfigurationProperties(prefix = "yichen")
@Data
public class HelloAutoConfigurationProperties {

    private String name;

    private String gender;
}
```



**@ConfigurationProperties**会读取类路径下application文件的类容，prefix则为前缀



### application.yml

```yml
yichen:
  gender: 男
  name: 一臣
```



### HelloService

```java
@Data
public class HelloService {

    private String name;
    private String gender;

    public String say() {
        return this.getName() + "/" + this.getGender();
    }
}
```



其实一个简单的starter就写好了，我们可以将这个工程mvn install，让其他的工程进行依赖

假设这是其他的一个工程，我们可以从IOC中获取HelloService，执行say()方法，看看是否得到了我们想要的结果

```java
@Component
public class AppRunnerImpl implements ApplicationRunner {

    @Autowired
    private HelloService helloService;

    @Override
    public void run(ApplicationArguments args) {
        System.out.println(helloService.say());
    }
}
```



# 再读源码

**AutoConfigurationImportSelector**是我们重点关注的类，



``` java

// 获取候选者的配置
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        List<String> configurations = SpringFactoriesLoader.loadFactoryNames(this.getSpringFactoriesLoaderFactoryClass(), this.getBeanClassLoader());
        Assert.notEmpty(configurations, "No auto configuration classes found in META-INF/spring.factories. If you are using a custom packaging, make sure that file is correct.");
        return configurations;
    }
```



```
// 注意，这里返回的class，会作为之后读取spring.factories文件的key
protected Class<?> getSpringFactoriesLoaderFactoryClass() {
    return EnableAutoConfiguration.class;
}
```



**SpringFactoriesLoader#loadSpringFactories**这里就是加载配置文件的入口

```java
private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
    MultiValueMap<String, String> result = (MultiValueMap)cache.get(classLoader);
    if (result != null) {
        return result;
    } else {
        try {
            // 读取配置文件，可能有多个，所以这里是一个枚举集合
            Enumeration<URL> urls = classLoader != null ? classLoader.getResources("META-INF/spring.factories") : ClassLoader.getSystemResources("META-INF/spring.factories");
            LinkedMultiValueMap result = new LinkedMultiValueMap();

            while(urls.hasMoreElements()) {
                URL url = (URL)urls.nextElement();
                UrlResource resource = new UrlResource(url);
                Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                Iterator var6 = properties.entrySet().iterator();

                while(var6.hasNext()) {
                    Entry<?, ?> entry = (Entry)var6.next();
                    String factoryTypeName = ((String)entry.getKey()).trim();
                    String[] var9 = StringUtils.commaDelimitedListToStringArray((String)entry.getValue());
                    int var10 = var9.length;

                    for(int var11 = 0; var11 < var10; ++var11) {
                        String factoryImplementationName = var9[var11];
                        result.add(factoryTypeName, factoryImplementationName.trim());
                    }
                }
            }

            cache.put(classLoader, result);
            return result;
        } catch (IOException var13) {
            throw new IllegalArgumentException("Unable to load factories from location [META-INF/spring.factories]", var13);
        }
    }
}
```



**AutoConfigurationImportSelector#getAutoConfigurationEntry**

```java
protected AutoConfigurationImportSelector.AutoConfigurationEntry getAutoConfigurationEntry(AutoConfigurationMetadata autoConfigurationMetadata, AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    } else {
        AnnotationAttributes attributes = this.getAttributes(annotationMetadata);
        List<String> configurations = this.getCandidateConfigurations(annotationMetadata, attributes);
        // 删除掉冲突的类
        configurations = this.removeDuplicates(configurations);
        // 获取我们想要排除的类
        Set<String> exclusions = this.getExclusions(annotationMetadata, attributes);
        this.checkExcludedClasses(configurations, exclusions);
        // 删除掉我们排除的自动配置类
        configurations.removeAll(exclusions);
        // 过滤
        configurations = this.filter(configurations, autoConfigurationMetadata);
        this.fireAutoConfigurationImportEvents(configurations, exclusions);
        return new AutoConfigurationImportSelector.AutoConfigurationEntry(configurations, exclusions);
    }
}
```

