[redis.md](https://github.com/Hyp02/hm-dianping/files/12577092/redis.md)# hm-dianping
> 注意，以黑马点评来做教程
>
> 部署上线时运行路径是`/www/wwwroot/nginx-1.18.0/html/hmdp;`
>
> ![image-20230910172759789](../../AppData/Roaming/Typora/typora-user-images/image-20230910172759789.png)
>
> 访问路径是：`http://116.204.87.237/index.html`

![image-20230910172729145](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910172729145.png)

# redis笔记

redis是一种键值对类型的数据库，是一种NoSQL数据库

# 初识Redis

## 认识NoSql![image-20230909211130736](../../AppData/Roaming/Typora/typora-user-images/image-20230909211130736.png)

## 认识Redis

Redies诞生于2009年，全称是Remote Dictionary Server 远程词典服务器，是一个基于内存的键值型NoSql数据库

### 特征

- 键值（Key ---- Value）型，value支持多种不同数据结构，功能丰富
- 但线程，每个命令具备原子性（但线程）
- 低延迟，速度快，（**基于内存**，IO多路复用，良好的编码）
- 支持数据持久化
- 支持主从集群，分片集群
- 支持多语言客户端

## 安装Redis

- 因为Redis是居于C语言编写的，所以需要gcc环境

`yum install -y gcc tcl`

- 拿到redis6的安装包

`解压到 /opt`

- 进入redis目录

`cd /opt/redis6.……`

- 运行编译命令

`make && make install` 等待……

- 启动redis `redis -server` 前台运行，不推荐

- 修改配置文件，将他改为后台启动

- 先将配置文件复制一份  使用`cp`命令 他的配置文件在下面这个地方**（我的）**

![image-20230909210757908](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230909210757908.png)

![image-20230830204752728](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230830204752728.png)

![image-20230830204822093](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230830204822093.png)

## redis连接

- redis-cli -a 密码
- GUI   
- 多语言SDK

### 连接问题

连接失败

`firewall-cmd --query-port=6379/tcp`如果返回No执行`firewall-cmd --add-port=6379/tcp`返回success,重新连接

参考文章：https://blog.csdn.net/gagadack/article/details/123267417

# Redis常用命令

忘了没关系，忘了就查，不用背

redis官网命令：https://redis.io/commands/

中文版：http://www.redis.cn/commands.html

![image-20230830223600125](../../AppData/Roaming/Typora/typora-user-images/image-20230830223600125.png)

## Redis通用命令

- **set key value**
- **get key**
- **keys pattern** 模糊搜索多个key,性能较差，生产环境（尤其是主节点）不建议使用
- **del key**
- **exists key** 判断key是否存在
- **expire key** 设置key的过期时间

  - **应用：**验证码的存活时间 可节省内存，还可以提高验证码的安全性
- **ttl key** 查询剩余存活时间，**-1表示未设置过期时间为  -2表示已过期**


## key的命名

Redis的key允许有多个单词形成层级结构，多个单词之间用`:`隔开，形式如下：

![image-20230904084927020](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230904084927020.png)

这个格式并非固定，也可以根据自己的需求来删除或添加词条。

![image-20230904085057388](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904085057388.png)

### 实例

存放数据

![image-20230904090801040](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904090801040.png)

使用可视化界面查看

![image-20230904090849236](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904090849236.png)

![image-20230904090903808](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904090903808.png)

 

# Redis数据类型

## String类型

### String类型的常用命令

![image-20230903202914409](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230903202914409.png)

![image-20230904091632197](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904091632197.png)

## Hash类型

![image-20230904091846124](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904091846124.png)

### Hash类型的常见命令

![image-20230904091913018](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904091913018.png)

## list类型

![image-20230904094441176](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904094441176.png)

Redi中的list类型和java中的LinkedList类似，可以看做是一个双向链表，既可以支持正向检索也可以支持反向检锁

特征也和LinkedList类似

- 有序
- 元素可以重复
- 插入和删除快
- 查询速度一般

### list类型常用命令

![image-20230904093743222](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904093743222.png)

 ## Set类型

Redis的Set结构与Java的HashSet类似，可以看做是一个value为null的HashMap。因为也是一个Hash表，因此与**HashSet类似的特征：**

- 无序
- 元素不可重复
- 查找快
- 支持交集，并集，差集等功能

### Set类型常用命令

![image-20230904104729930](../../AppData/Roaming/Typora/typora-user-images/image-20230904104729930.png)

## SortedSet

![image-20230904104914484](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904104914484.png)

### SortedSet常见命令

![image-20230904104940778](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904104940778.png)

# Rdeis的Java客户端

## jedis

https://github.com/redis/jedis

创建一个Maven工程

将jedis依赖导入

![image-20230904114646054](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904114646054.png)

### Jedis直连方式

```java
public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 建立连接
        jedis = new Jedis("192.168.48.134", 6379);
        // 设置密码
        jedis.auth("123456");
        // 选择库
        jedis.select(0);
    }

    @Test
    void testString() {
        String hyp = jedis.set("user:1", "hyp");
        System.out.println(hyp);
        String s = jedis.get("user:1");
        System.out.println("user:1=" + s);
    }

    @Test
    void testHash() {
        // 插入一个
        jedis.hset("hash:user:1", "name", "hyp");
        // 获取一个
        Map<String, String> hgetAll = jedis.hgetAll("hash:user:1");
        System.out.println("hash:user:1=" + hgetAll);

        // 插入多个
        Map map = new HashMap<>();
        map.put("name", "hyp1");
        map.put("age", "19");
        jedis.hmset("hash:user:2", map);

        // 获取指定字段
        System.out.println(jedis.hmget("hash:user:2", "name","age"));

        // 获取所有的k v
        Map<String, String> hmget = jedis.hgetAll("hash:user:2");
        System.out.println("hash:user:2=" + hmget);
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
```

### jedis连接池

jedis本身是不安全的！并且频繁的创建和销毁连接会有性能损耗，因此我们推荐大家使用Jedis连接池代替jedis直连方式

```java
public class JedisConnectionFactory {
    private static JedisPool jedisPool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 最大连接数
        poolConfig.setMaxTotal(8);
        // 最大空闲连接
        poolConfig.setMaxIdle(8);
        // 最小空闲连接 一段时间后还是没有人连接就将连接数设置为最小连接数
        poolConfig.setMinIdle(0);
        // 最大等待时长
        poolConfig.setMaxWaitMillis(1000);
        // 创建连接对象
        jedisPool = new JedisPool(poolConfig, "192.168.48.134",
                6379, 1000, "123456");
    }
    public static Jedis getJedisPool(){
        return jedisPool.getResource();
    }

}
```

```java
public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 建立连接
        // jedis = new Jedis("192.168.48.134", 6379);
        // 设置密码
        jedis = JedisConnectionFactory.getJedisPool();

        jedis.auth("123456");
        // 选择库
        jedis.select(0);
    }
    ……     ……
}
```

## SpringData

### 介绍

![image-20230904153447913](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904153447913.png)

![image-20230904153609538](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904153609538.png)

### 使用

使用IDEA脚手架创建springBoot项目，添加springData-redis依赖

也可手动动添加

```xml
<!--reids依赖-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--commons-poll-->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 示例

#### 使用redisTemplate[不建议]

> 1.  自定义RedisTemplate
> 2.  修改RedisTemplate的序列化器为GenericJackson2JsonRedisSerializer

导入上面的依赖后，就可以使用redisTemplate了

- 操作String类型

```java
@SpringBootTest
class SpringDataDemoApplicationTests {
    @Resource
    private RedisTemplate redisTemplate;
    @Test
    void testString() {
        // 存入数据 [字符串类型]
        redisTemplate.opsForValue().set("name","胡噶尔");
        // 获取数据
        Object name = redisTemplate.opsForValue().get("name");
        // 输出
        System.out.println(name);
    }

}
```

使用`redisTemplate.opsForValue().set)()`方法存放的key和value并不是一个字符串类型，会导致存到redis中的数据出现问题![image-20230904164826894](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904164826894.png)

解决方式

- 编写配置类

​		配置`redisTemplate`bean,设置k和v的序列化方式

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate连接对象
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);

        // 创建Json序列化工具
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // 设置key的序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // 设置value的序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        //初始化参数和初始化工作
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```

重新测试，这次增加将对象作为v传入【User】

```java
@SpringBootTest
class SpringDataDemoApplicationTests {
    @Resource
    private RedisTemplate redisTemplate;
    @Test
    void testString() {
        // 存入数据 [字符串类型]
        redisTemplate.opsForValue().set("name","胡噶尔");

        // 存入数据 [对象类型]
        User user = new User("韩永鹏", 20);
        redisTemplate.opsForValue().set("user",user);
        // 获取数据
        Object name = redisTemplate.opsForValue().get("name");
        Object user1 = redisTemplate.opsForValue().get("user");
        // 输出
        System.out.println(user1);
        System.out.println(name);
    }

}
```

结果

![image-20230904165959716](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904165959716.png)

> 以上 方法有一个问题
>
> 他要保证可以反序列化，就要保存对象的`@class`，如果数据量巨大，就会造成资源的浪费，
>
> 所以不推荐使用上述方法

#### 使用StringRedisTemplate[推荐]

> 1. 使用StringRedisTemplate
> 2. 写入Redis时，手动把对象序列化为Json
> 3. 读取Redis时，手动把JSON反序列化为对象

**这种方法需要手动反序列化**

使用StringRedisTemplate就不需要使用配置类进行bean的配置了

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdatademo.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;


import javax.annotation.Resource;

@SpringBootTest
class SpringRedisTemplateTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 操作String类型
    @Test
    void testString() {
        // 存入数据 [字符串类型]
        stringRedisTemplate.opsForValue().set("name", "胡噶尔");
        // 获取数据
        Object name = stringRedisTemplate.opsForValue().get("name");
        // 输出
        System.out.println(name);
    }

    // 存入数据 [对象类型]
    @Test
    void saveObj() throws JsonProcessingException {
        User user = new User("韩永鹏", 20);
        // 使用ObjectMapper将对象序列化
        String jsonByUser = objectMapper.writeValueAsString(user);

        // 存放
        stringRedisTemplate.opsForValue().set("user", jsonByUser);
        Object user1 = stringRedisTemplate.opsForValue().get("user");
        // 输出
        System.out.println(user1);
        // 手动反序列化输出
        User readValue = objectMapper.readValue(jsonUser, User.class);
        System.out.println(readValue);
        

    }

}

```

代码运行结果

![image-20230905195945005](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230905195945005.png)

**结果**

**![image-20230904182042437](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230904182042437.png)**

# Redis实战[1]

![image-20230905202425725](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230905202425725.png)

## 短信验证码登录

**使用黑马点评作为测试项目**

将黑马点评的后端源码导入到IDEA中，启动

将提供的前端代码复制到一个没有中文路径的文件中

在nginx.exe所在的窗口输入`start nginx.exe`

浏览器输入`localhost:端口号`打开前端项目

![image-20230905211111609](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230905211111609.png)

### 基于session用户登录

![image-20230906192755195](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230906192755195.png)

**发送验证码**

- 先判断输入的手机号码是否符合格式

- 如果符合格式，使用`RandomUtils.randomNumbers(n)`这个方法生成一个n位数字的验证码

- 将这个验证码存储在session中

- 因为短信发送需要第三方平台，这里使用日志记录

```java

/**
* 发送手机验证码
*/
@PostMapping("code")
public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
    // TODO 发送短信验证码并保存验证码
    Result result = userService.sendCode(phone, session);
    return result;
}
public Result sendCode(String phone, HttpSession session) {
    // 校验手机号判断是否符合
    if (RegexUtils.isPhoneInvalid(phone)) {
        // 不符合
        return Result.fail("手机号格式错误！！");
    }
    // 符合(生成6位数字验证码并保存在session中)
    String code = RandomUtil.randomNumbers(6);
    // 保存验证码
    session.setAttribute("code", code);
    // 发送验证码【因为发送验证码需要调用第三发Api,这里使用日志记录】
    log.debug("发送验证码成功 验证码为：{}", code);
    // 返回成功信息
    return Result.ok();
}
```

**使用验证码登录**

![image-20230906213212496](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230906213212496.png)

```java

/**
* 登录功能
* @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
*/
@PostMapping("/login")
public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
    // TODO 实现登录功能
    Result result = userService.userLogin(loginForm, session);
    return result;
}

@Override
public Result userLogin(LoginFormDTO loginForm, HttpSession session) {
    String phone = loginForm.getPhone();
    String code = loginForm.getCode();
    // 校验
    if (StringUtils.isAnyBlank(phone)) {
        Result.fail("手机号不能为空");
    }
    if (StringUtils.isAnyBlank(code)) {
        Result.fail("验证码不能为空");
    }
    if (RegexUtils.isPhoneInvalid(phone)) {
        Result.fail("手机号格式不正确");
    }
    // 检验验证码是否正确
    Object cacheCode = session.getAttribute("code");
    if (!code.equals(cacheCode.toString())) {
        Result.fail("验证码不正确");
    }
    // 查询用户是否存在
    User user = query().eq("phone", phone).one();
    if (user == null){
        // 不存在自动注册
        user = createUserWithPhone(phone);
    }
    // 保存用户到session
    session.setAttribute("user", user);
    return Result.ok();
}
```

**登录态校验**

![image-20230906215734975](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230906215734975.png)

```java
 @GetMapping("/me")
    public Result me(HttpSession session){
        // TODO 获取当前登录的用户并返回
        User user = (User) session.getAttribute("user");
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        UserHolder.saveUser(userDTO);
        Result ok = Result.ok(user);
        return ok;
    }
```

> 注意：
>
> 这种方式存在一个问题
>
> 我们的项目有很多Controller，那每个Controller中都要写这样一段校验登录态的代码吗
>
> 这样做会使代码非常冗余，不利于维护
>
> 所以我们需要使用一个拦截器，让拦截器实现用户登录状态的校验
>
> 想要请求Controller，就要经过拦截器

**使用拦截器拦截Controller请求**

![image-20230906215931241](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230906215931241.png)

#### 编写拦截器

- 功能：
  - 获取session中存放的登录用户的信息
  - 判断这个用户是否存在，也就是用户是否登录
    - 登陆了 放行
    - 未登录 不放行
  - 将这个用户存放在ThreadLocal中，供获取登录用户信息使用

```java
public class LoginInterceptor implements HandlerInterceptor {
    // 前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session中的信息
        User user =(User) request.getSession().getAttribute("user");
        // 是否存在
        if (user == null){
            response.setStatus(401);
            return false;
        }
        // 将用户存放在ThreadLocal中
        UserHolder.saveUser(user);
        return true;
    }
    // 后置拦截
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除登录用户
        UserHolder.removeUser();
    }
}
```



- 注册拦截器
  - 创建新的配置类`MvcConfig`
  - 实现 WebMvcConfigurer接口
  - 配置放行接口，或不放行的接口

```java
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置不需要拦截的接口
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**" ,
                        "/shop-type/**",
                        "/voucher/**",
                        "/upload/**",
                        "blog/**",
                        "/user/code",
                        "/user/login"
                );
    }
}
```

- 最后将用户信息返回

```java
 @GetMapping("/me")
    public Result me(HttpSession session) {
        // 获取用户信息返回
        User user = UserHolder.getUser();
        return Result.ok(user);

    }
```

> 注意：
>
> 这里返回的用户是用户的所有信息，未脱敏，这是不安全的
>
> 所以要进行脱敏

**用户脱敏**
用户脱敏有两种方法

> 这里采用了第二种方式

- **第一种：**UserServiceImpl中定义一个`safetyUser(User user)`方法，进行脱敏

```java
// 用户脱敏【未使用的方法】
    private User safetyUser(User user) {
        User safetyUser = new User();
        safetyUser.setNickName(user.getNickName());
        safetyUser.setIcon(user.getIcon());
        safetyUser.setId(user.getId());
        return safetyUser;
    }
```

- **第二种：**创建一个UserDTO类,字段是可以返回的字段

```JAVA
/**
 * 脱敏用户类
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
```

- 然后可以在登录的时候就存放UserDTO类型的用户
- ![image-20230906230306004](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230906230306004.png)

**至此 基于session方式登录就完成了**

**但是！！！ 基于session'的方式有一个问题，**

**那就是，Session共享问题**

### session共享问题

**session共享问题是：** 多台tmocat服务器不共享session存储空间，当请求换到不同的tomcat服务器时，会导致数据丢失 	

> 有什么可以替代session的方案？
>
> 他需要满足
>
> - 数据共享
> - 内存存储（内存访问速度超快）
> - key  value存储
>
> **答案就是Redis!** 因为Redis独立于tomcat ,所有的tomcat服务器都可以访问Redis,这解决了数据丢失的问题，并且Redis访问速度非常快，和session相同，也是k v 结构存储数据的

### 基于Redis实现共享Session登录

![image-20230907193501824](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230907193501824.png) 



- 先修改发送验证码业务，需要将验证码保存在redis中
  - 只需要更改验证码的保存位置，并且设置过期时间 。
  - 原来是保存在session中的，现在使用Redis将验证码保存在String中

![image-20230907210610072](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230907210610072.png)

- 再修改登录业务，将登录的用户保存在Redis中
  - 生成一个`token`,使用UUID生成
  - 要生成一个登录`userToken`，使用这个userToken作为Redis的k将登录的用户保存在redis中，最终用于从Redis中获取登录的用户
  - 定义这个`userToken`格式为：`"login:token"+token`
  - 并且设置过期时间

> ```
> /**
>  * 这里设置的是无论用户怎么操作，一到30分钟就会过期
>  * 但是我们需要的是当用户没有任何操作时，超过30分钟，才过期
>  * 如果用户进行操作，这个过期时间就会一直刷新    
>  * 但是我们怎么知道用户到底在不在操作呢？
>  *      答：当用户操作时就会触发拦截器，所以判断用户是否操作就是判断拦截器是否触发
>  */
> stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
> ```

![image-20230907211653909](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230907211653909.png)

- 在拦截器中修改并更新用户登录状态过期时间
  - 先获取请求的token
  - 将这个token对应的Redis数据取出来，因为当时存的时候存的是map类型，所以这里取出的也是map类型
  - 这里还需要将取出的用户存在`ThreadLocal`中，所以还要将取出来的数据转换成UserDTO类型存放
  - 最后刷新用户过期时间

```java
 // 前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session中的信息
        //UserDTO userMap =(UserDTO) request.getSession().getAttribute("user");
        // 是否存在
        // 获取token
        String token = request.getHeader("authorization");
        if (StringUtils.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        String userKey = RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        // 在redis中存储的是Map类型，所以要在这将Map类型转换为User类型再存储在ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 将用户存放在ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 刷新有效时间
        stringRedisTemplate.expire(userKey, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return true;
    }
```

> 这里有一个注意的点
>
> 因为这个拦截器是在webConfig这个配置类中注册并生效的，并且在配置类中，拦截器是new出来的，所以注入StringRedisTemplate时不能使用注解进行自动注入，需要使用构造函数进行注入
>
> ![image-20230907212240745](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230907212240745.png)![image-20230907212437425](../../AppData/Roaming/Typora/typora-user-images/image-20230907212437425.png)

这其中使用到的key的关系

因为验证码是对应手机号使用的，所以存放验证码的Redis数据类型的k应该使用`"前缀"+手机号`

获取登录用户时：每个登录的用户应该有一个唯一的token 进行获取用户，存放用户的Redis数据类型选择使用Hash类型，

那么为什么不选择使用手机号而选择UUTD作为key存储呢，原因是如果使用了手机号,Redis就会将手机号作为token的一部分发送给前端，**从而产生安全问题**

**注意，这里产生了一个问题，就是拦截器不是拦截所有的请求，如果用户一直访问的是不需要拦截的业务，那么就不会进入拦截器，也就是说不会进行用户登录状态有效时间的刷新，要解决这个问题，需要定义两层拦截器**

- 第一层拦截器`ReferenceInterception`拦截所有请求，将用户登录态时间刷新放在这个拦截器中
  - 查询token,无论有没有登录的用户都放行，并且将token对应的数据存放在ThreadLocal中
- 第二层拦截器`LoginInterception`拦截需要拦截的请求，并且查询ThreadLocal
  - 如果ThreadLocal中没有数据，拦截这个请求
  - 入股ThreadLocal中有十数据，放行这个请求
- 最后调节这连个拦截器的执行顺序，有一个属性是Order 越小先执行 默认都是0 按照添加顺序执行 可以给第一个添加`.order(0)`第二个添加`.order(1)`

![image-20230907221214304](../../AppData/Roaming/Typora/typora-user-images/image-20230907221214304.png)

```java
/**
	第一层
**/
public class RefreshInterceptor implements HandlerInterceptor {
    //@Resource 注意这里不能使用注解，因为拦截器在webConfig中是new出来的，不是Spring生成的,所以要使用构造器注入
    private StringRedisTemplate stringRedisTemplate;

    public RefreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session中的信息
        //UserDTO userMap =(UserDTO) request.getSession().getAttribute("user");
        // 是否存在
        // 获取token
        String token = request.getHeader("authorization");
        if (StringUtils.isBlank(token)) {
            return true;
        }
        String userKey = RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);
        if (userMap.isEmpty()) {
            return true;
        }
        // 在redis中存储的是Map类型，所以要在这将Map类型转换为User类型再存储在ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 将用户存放在ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 刷新有效时间
        stringRedisTemplate.expire(userKey, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return true;
    }

    // 后置拦截
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除登录用户
        UserHolder.removeUser();
    }
}

/**
	第二层
**/
public class LoginInterceptor implements HandlerInterceptor {
  
    // 前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否有登录的用户
        // 有登录的用户放行，没有拦截
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
    // 后置拦截
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除登录用户
        UserHolder.removeUser();
    }
}

/**
	配置类
**/
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置不需要拦截的接口
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**" ,
                        "/shop-type/**",
                        "/voucher/**",
                        "/upload/**",
                        "blog/**",
                        "/user/code",
                        "/user/login"
                ).order(1);
        // 刷新用户登录态的拦截器拦截所有请求
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);

    }
}

    
```

**短信验证码登录完整完成**

# Redis缓存

## 什么是缓存

缓存就是数交换的缓冲区（称作Cache）是存储数据的临时地方，一般读写性能较高。

### 缓存的作用与成本

**作用**

- 降低后端负载
- 提高读写效率，降低响应时间

**成本**

- 数据一致性成本
- 代码维护成本
- 运维成本

## 添加Redis缓存

### 实战

**给查询商铺信息添加缓存**

添加缓存流程

<img src="../../AppData/Roaming/Typora/typora-user-images/image-20230909145229381.png" alt="image-20230909145229381" style="zoom:200%;" />

**代码实现**

- 先找到ShopController，将控制层的返回代码修改

![image-20230909153711257](../../AppData/Roaming/Typora/typora-user-images/image-20230909153711257.png)

- 在店铺的server层中书写业务代码

```java
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 注入操作Redis的对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询店铺信息并使用redis缓存
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 命中 返回
        if (StringUtils.isNotBlank(shopJson)){
            // 转换为对象后返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 未命中 在数据库中查
        // 数据库中不存在 返回错误信息【getById是接口中的方法】
        Shop shop = getById(id);
        if (shop == null){
            return Result.fail("对不起，该店铺已消失");
        }
        String toJsonShop = JSONUtil.toJsonStr(shop);
        // 数据库中存在 先保存在redis中再返回
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,toJsonShop);
        return Result.ok(shop);
    }
}
```

- 刷新浏览器查询店铺

  第一次查询因为店铺没有缓存，所以请求时间长，并且查询了数据库

![image-20230909154141864](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230909154141864.png)

![image-20230909154227216](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230909154227216.png)

​	 第二次查询刷新浏览器，因为第一次请求后端将数据库中的数据已经写到了缓存中，所以速度得到极大的优化，并且没有访问数据库

![image-20230909154406066](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230909154406066.png)

![image-20230909154441208](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230909154441208.png)

**给首页店铺列表添加缓存**

1.查询redis

![image-20230909173015813](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230909173015813.png)

2.命中返回

![image-20230909173027034](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230909173027034.png)

3.未命中，查询数据库

![image-20230909173047044](C:\Users\Han\AppData\Roaming\Typora\typora-user-images\image-20230909173047044.png)

- 查询数据库
  - 数据不存在，返回错误信息

![image-20230909173141271](../../AppData/Roaming/Typora/typora-user-images/image-20230909173141271.png)

- 数据存在，添加redis，返回数据

![image-20230909173153996](../../AppData/Roaming/Typora/typora-user-images/image-20230909173153996.png)

```java
@Override
    public Result queryShopList() {
        // 查询redis
        String listJson = stringRedisTemplate.opsForValue().get(RedisConstants.CATCH_SHOP_LIST);
        // 命中返回
        if (listJson != null) {
            // 将得到的json字符串转换为list
            List list = JSONUtil.parseArray(listJson);
            return Result.ok(list);
        }
        // 未命中 查询数据库并且排序
        List<ShopType> shopSort = query().orderByAsc("sort").list();
        // 数据库中不存在，返回错误信息
        if (shopSort == null) {
            return Result.fail("未查找到店铺类型");
        }
        // 数据存在
        // 将数据库中数据转化为json
        String shopJson = JSONUtil.toJsonStr(shopSort);
        //写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CATCH_SHOP_LIST, shopJson);
        // 返回数据
        return Result.ok(shopSort);
    }
```

> 这里的难点也就是数据类型的选择，这里我选择的是使用String类型进行存储。
>
> 将数据库中取出的List数据通过`JSONUtil.toJsonStr()`方法转化成Json字符串，将Json字符串写入redis中。

## 缓存更新策略

**为了解决数据库中数据与缓存数据不一致的问题**

![image-20230909225821338](assets/image-20230909225821338.png)

缓存更新策略的选择

****

![image-20230909231042187](assets/image-20230909231042187.png)

先操作缓存还是先操作数据库？这里有一个线程安全问题，

**先删除缓存：**

- 因为操作数据库是要连接数据库进行操作的，速比起操作缓存更慢，

- 如果存在两个线程，第一个线程将缓存删除后，突然有第二个线程进来进行缓存的读取，

- 因为缓存已经删除，所以第二个线程缓存未命中，直接去查数据库，将未修改的数据查出并写入缓存，

- 接下来第一个线程才去修改数据库，这就会导致缓存里面是还未修改的数据，数据库中是已经修改了的新数据，发生了缓存与数据库数据不一致的问题
- 因为数据库操作的速度远远小于缓存的操作，所以在第一个线程的缓存删除完之后和修改数据库之前这一段空隙中，第二个线程的缓存读取已经完成，所以这种方法发生问题的概率是很大的

**先修改数据库**

- 如果先修改数据库。第一个将数据库中数据修改后，紧接着要删除缓存
- 这时候有第二个线程来读取缓存，因为缓存还未被删除，所以第二个线程查到了旧数据，第二个线程完成。这时候发生了问题，数据库中数据已经被修改，但缓存中的数据是旧数据，发生了缓存和数据库中数据不一致的问题
- 接下来第一个线程删除缓存
- 但是 第一个线程修改数据库后紧接着就是删除缓存，因为缓存的操作是非常快的，在数据库修改和删除缓存的空隙中，时间是非常短的，要在这段时间内实现第二个线程，进行读取缓存，如果缓存未命中【**缓存在这时候恰好失效了，可能的原因是活过期等**】，还要去查数据库，做这么多工作，要在极短时间内完成是几乎不可能的，所以这种方法发生错误的概率比第一种小

**缓存更新策略的最佳实战方案**

- 第一只性需求：使用Redis自带的内存淘汰机制
- 高一致性需求：主动更新，并以超时作为兜底方案

![image-20230910134630562](../../AppData/Roaming/Typora/typora-user-images/image-20230910134630562.png)

**解决店铺信息更新后缓存数据一致性问题**

> 修改数据库中店铺信息后，因为缓存的原因，用户界面看到的数据还是旧数据，这时就要更新缓存

- 设置店铺信息缓存的失效时间
- 修改数据库中店铺信息
- 删除原来【key】的缓存
- 为确保数据的一致性，添加事务@Transactional注解

```java
@Override
@Transactional //添加事务
public Result updateShopById(Shop shop) {
    Long id = shop.getId();
    if (id == null) {
        return Result.fail("餐厅不存在");
    }
    // 修改数据库
    boolean b = updateById(shop);
    if (!b){
        return Result.fail("更新店铺信息失败");
    }
    // 删除缓存
    stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
    return Result.ok();
}
```



## 缓存穿透

**缓存穿透**是指客户端请求的数据在缓存和数据库中都不存在，这样缓存就永远不会生效，这些请求都会打到数据库中

**解决方案：**

- 缓存空对象
  - 优点：实现简单，维护方便
  - 缺点
    - 额外的内存消耗
    - 可能造成短期的不一致
    - 这两个问题都是可以解决的，可以设置返回空缓存，将空数据写到缓存中，并且设置TTL。

![image-20230910135738536](../../AppData/Roaming/Typora/typora-user-images/image-20230910135738536.png)

- 布隆过滤
  - 优点：内存占用较少，没有多余key
  - 缺点：
    - 实现复杂
    - 有一定缓存穿透风险

![image-20230910140608345](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910140608345.png)

### **给查询商铺信息解决缓存穿透 **

![image-20230910141010823](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910141010823.png)

- 查到的数据为null的话，将空值写入缓存中

![image-20230910144906440](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910144906440.png)

- 读取时，如果读到了空缓存说明店铺不存在，不能再去请求数据库了，直接结束
- 解决了请求店铺时缓存穿透问题

![image-20230910145022994](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910145022994.png)

**总结：**

![image-20230910153823756](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910153823756.png)

## 缓存雪崩

**缓存雪崩：**是指在同一段时段内大量的缓存Key同时失效或者Redis服务宕机，导致大量请求达到数据库，带来巨大压力

- **Key失效导致的雪崩**
- **redis宕机导致的雪崩**
  - 给不同的keydeTTL添加随机值
  - 利用Redis集群提高服务的可用性
  - 给缓存业务添加降级限流策略
  - 给业务添加多级缓存

![image-20230910203248542](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910203248542.png)

## 缓存击穿

**缓存击穿问题**也叫热点Key问题，就是一个高并发访问并且**缓存重建业务较复杂**的Key突然失效了，无数的请求访问会在瞬间给数据库带来巨大的冲击

- 在第一个线程重建缓存的时候，因为缓存重建业务复杂，所需时间长，不能快速重建缓存。就会导致后续所有线程都无法命中缓存，都会去直接操作数据库，最终所有请求都会落到数据库上

![image-20230910204429920](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910204429920.png)

常见的解决方案有两种：

- 互斥锁

​		线程一查询缓存，未命中 获取互斥锁成功 重建缓存的过程中，这时有一个线程二也来查询缓存，但是因为线程一将互斥锁拿走还未释放，所以线程二只能进行等待，如果线程数量巨大，就会导致性能下降

![image-20230910205239152](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910205239152.png)

- 逻辑过期
  - 有一个线程一查询缓存，这个缓存是永久的，但是有一个逻辑过期时间 逻辑过期时间是当前时间+10秒【不一定是10，但是要远大于缓存重建的时间】在这段时间内要保证缓存重建成功


​		1、发现逻辑时间已经过期，获取互斥锁后进行缓存重建，它会重新开启一个新线程**去执行缓存重建逻辑**，主线程将旧数据直接返回，

​		2、这时候如果有第二个线程来查询缓存，发现一进过期并且获取互斥锁失败，直接返回旧数据，**不进行缓存重建**，当有一个线程是在第一个线程的副线程执行完之后进来的，他就能直接获取数据

​	    3、 因为已经将店铺信息提前加入到缓存中了，并且实际过期时间理论是永久的，如果没有查询到缓存说明这个数据是真的不存在，所以直接返回null结束

![image-20230910205302498](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230910205302498.png)

### **基于互斥锁方式解决缓存击穿问题**

需求：修改根据Id查询商铺的业务，基于互斥锁方式来解决缓存击穿问题

![image-20230911102028082](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911102028082.png)

- 如果查询缓存时没有命中缓存
  - 获取互斥锁
  - 未获得互斥锁
    - 休眠一定时间
    - 重新获取互斥锁
  - 获得了互斥锁
    - 连接数据库重建缓存	
    - 释放互斥锁

**获取互斥锁**

通过Redis中String类型的setnx操作

![image-20230911102439898](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911102439898.png)

通过这个命令添加一个值，如果这个值存在，说明正在有人使用这个互斥锁

```java
	/**
     * 获取锁
     * 本质就是看是否另一个线程能将数据插入，如果插入不了就说明有人使用
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        // 插入一个值
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // 返回是否成功
        return BooleanUtil.isTrue(aBoolean);

    }
```

通过删除这个由SETNX插入的值来释放锁

本质就是如果这个key不存在，另一个使用者就可以插入值，也就说明没有人使用这个锁

```java
 /**
     * 释放锁
     *
     * @param key
     * @return
     */
    private boolean delLock(String key) {
        Boolean aBoolean = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(aBoolean);

    }
```

- 给通过id获取店铺信息增加互斥锁来解决可能存在的缓存击穿问题
- ![image-20230911103145332](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911103145332.png)

![image-20230911103056960](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911103056960.png)

### **基于逻辑过期方式解决缓存击穿问题**

**注意：**因为设置的redis的有效时间理论上是永久的并且将这个数据已经提前写入缓存 ，所以如果未命中缓存，就说明这个店铺是不存在的，直接返回空

![image-20230911104134532](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911104134532.png)

> 这里学到一个编程技巧,如果在开发过程猴子那个,要向给某个pojo类增加一个字段,肯定要修改业务代码.非常不好
>
> 可以定义一个新的类pojo类,在这个类中添加你要增加的字段,并设置一个Object类型的Data字段。
>
> 我想要在Shop类中添加一个过期时间expiredTime
>
> ![image-20230911111407475](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911111407475.png)
>
> ![image-20230911111440849](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911111440849.png)

1. 先查询Redis

   ![image-20230911152748128](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911152748128.png)

   1. 未命中 返回null 结束 原因上面已经提到

      ![image-20230911152821936](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911152821936.png)

   2. 命中 

      1. 查看逻辑时间是否过期

         1. 未过期

            1. 返回缓存中的数据

               ![image-20230911152841922](../../AppData/Roaming/Typora/typora-user-images/image-20230911152841922.png)

         2. 过期

            1. 重建缓存

               1. 获取互斥锁

                  ![image-20230911152903249](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911152903249.png)

                  1. 成功

                     1. 开启独立线程重建缓存

                     2. 释放互斥锁

                        ![image-20230911152921117](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911152921117.png)

                  2. 失败

                     1. 直接返回旧缓存数据

                        ![image-20230911152930172](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911152930172.png)

> 新增店铺石否是热点判断
>
> ![image-20230911194424132](https://gitee.com/hyp02/typora_lmage/raw/master/img/image-20230911194424132.png)

```java
 @Override
    public Result queryById(Long id) {
        // 互斥锁解决缓存击穿
        //Shop shop = this.queryWithMutex(id);
        // 判断店铺是否是热点店铺
        Shop shop = null;
        if (Arrays.asList(HotShop.hotShopId).contains(id)) {
            // 指定逻辑过期解决高并发问题的缓存击穿
            shop = this.queryWithExpiredTime(id);
            if (shop == null){
                Result.fail("店铺不见了");
            }
            return Result.ok(shop);
        }
        // 如果不是热点店铺，并发不高
        // 空缓存解决缓存穿透
        shop = this.queryWithPassThorough(id);
        if (shop == null) {
            Result.fail("店铺不见了");
        }
        return Result.ok(shop);
    }
```

## 缓存工具封装





