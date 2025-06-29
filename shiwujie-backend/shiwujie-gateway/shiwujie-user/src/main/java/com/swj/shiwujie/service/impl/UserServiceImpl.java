package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.FamilyMapper;
import com.swj.shiwujie.mapper.UserMapper;
import com.swj.shiwujie.model.VO.UserLoginVO;
import com.swj.shiwujie.model.VO.UserVO;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.enums.UserCallStatusEnum;
import com.swj.shiwujie.model.request.UserRegisterRequest;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.JWTUtils;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.swj.shiwujie.constants.UserConstant.*;

/**
 * @author ldl
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2024-12-15 23:26:31
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;



    @Resource
    private FamilyMapper familyMapper;


    @Resource
    private RedisTemplate<String, Object> redisTemplate;




    /**
     * 用户一键登录注册
     *
     * @param userPhone
     * @return
     */
    @Override
    public UserLoginVO userLoginAndRegisterQuickly(String userPhone) {
        if(userPhone == null || userPhone.length() == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数错误");
        }
        // 检测手机号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPhone",userPhone);
        User user = this.getOne(queryWrapper);
        if(user != null){
            // 存在: 1. 生成令牌,存redis 2.用户信息脱敏返回
            log.debug("用户信息存在,一键登录");
            return this.getUserLoginVOByUser(user);
        }
        // 不存在: 1. 数据库添加用户 2. 生成令牌 3.用户信息脱敏返回
        log.debug("用户信息不存在,一键注册");
        // 1,数据库添加用户
        user = new User();
        user.setUserPhone(userPhone);
        String userAccount = this.generateUserAccount();
        user.setUserAccount(userAccount);
        user.setStatus(2);
        //设置密码为空字符串
        user.setUserPassword("");
        this.save(user);
        // 2生成令牌,加入redis,返回
        return this.getUserLoginVOByUser(user);

    }

    /**
     * 用户账号密码一键登录注册
     *
     * @param userRegisterRequest 用户的手机号与密码
     * @return 用户的脱敏信息与token
     */
    @Override
    public UserLoginVO userLoginAndRegister(UserRegisterRequest userRegisterRequest) {
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        //校验手机密码格式是否正确
        String userPhone = userRegisterRequest.getUserPhone();
        String userPassword = userRegisterRequest.getUserPassword();
        this.phoneVerify(userPhone);
        this.passwordVerify(userPassword);

        // 密码md5加密
        userPassword = DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());

        // 检测账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPhone",userPhone);
        User user = this.getOne(queryWrapper);

        if(user != null){
            // 存在: 生成令牌,存redis ,用户信息脱敏返回
            //检测密码是否存在,不存在不允许账号密码登录
            if(user.getUserPassword().length() == 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未设置密码");
            }
            if(!user.getUserPassword().equals(userPassword)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
            }
            log.debug("用户信息存在,账号密码登录");
            return this.getUserLoginVOByUser(user);
        }

        // 不存在: 1. 数据库添加用户 2. 生成令牌 3.用户信息脱敏返回
        log.debug("用户信息不存在,账号密码注册");

        // 1,数据库添加用户
        user = new User();
        user.setUserPhone(userPhone);
        user.setUserPassword(userPassword);
        String userAccount = this.generateUserAccount();
        user.setUserAccount(userAccount);
        user.setStatus(2);
        this.save(user);

        // 2生成令牌,加入redis,返回
        return this.getUserLoginVOByUser(user);

    }



    /**
     * 注销用户
     *
     * @param request
     * @return
     */
    @Override
    public UserVO removeUserById(HttpServletRequest request) {
        // 1. 从请求头中获取登录用户的ID
        Long loginUserId = LoginUtils.getCurrentUserId(request);
        // 2. 从数据库中查询用户信息
        User user = this.getById(loginUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户不存在");
        }

        //若该用户是家主就解散家庭
        Long familyId = user.getFamilyId();
        Family family = familyMapper.selectById(familyId);
        if(family!= null && family.getUserId().equals(loginUserId)){
            //从家庭中获取成员信息,并修改familyId然后更新数据库
            String userIdsString = family.getAddId();
            Gson gson = new Gson();
            List<Long> userIds = gson.fromJson(userIdsString, new TypeToken<List<Long>>(){}.getType());
            if(userIds == null || userIds.size() == 0){
                return null;
            }
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.in("id",userIds);
            List<User> userList = this.list(userQueryWrapper);
            userList.stream().map(user1 -> {
                user1.setFamilyId(0L);
                return user1;
            }).collect(Collectors.toList());
            this.updateBatchById(userList);
            user.setFamilyId(0L);

            //删除家庭
            int b = familyMapper.deleteById(familyId);
            if(b == 0){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
            }
        }
        boolean b = this.removeById(loginUserId);
        if (!b) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注销失败");
        }

        // 3. 删除登录用户的缓存
        redisTemplate.delete(LOGIN_USER_KEY + loginUserId);
        redisTemplate.delete(LOGIN_USER_TOKEN + loginUserId);

        // 4. 返回删除的用户信息
        return this.getUserVOByUser(user);
    }

    /**
     * 用户信息修改
     *
     * @param user
     * @param currentUserId
     * @return
     */
    @Override
    public UserVO updateUserById(User user, Long currentUserId) {
        if(user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户信息为空");
        }
        // 1. 从数据库中查询用户信息
        User updateUser = this.getById(currentUserId);
        if (updateUser == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户不存在");
        }
        // 2. 修改用户信息
        String userName = user.getUserName();
        if(userName != null && userName.length() > 0){
            updateUser.setUserName(userName);
        }
        //校验手机号和邮箱,如果不是之前的就需要校验
        String userPhone = user.getUserPhone();
        if(userPhone!=null && !userPhone.equals(updateUser.getUserPhone()) && userPhone.length() > 0 && this.phoneVerify(userPhone)){
            updateUser.setUserPhone(userPhone);
        }
        String userEmail = user.getUserEmail();
        if(userEmail!= null && !userEmail.equals(updateUser.getUserEmail())  && userEmail.length() > 0 && this.emailVerify(userEmail)){
            updateUser.setUserEmail(userEmail);
        }
        //如果修改了用户头像就修改用户头像
        String userUrl = user.getUserUrl();
        if(userUrl!= null && userUrl.length() > 0){
            updateUser.setUserUrl(userUrl);
        }
        //如果修改用户性别
        Integer gender = user.getGender();
        if(gender!= null && gender>=0 && gender<2){
            updateUser.setGender(gender);
        }
        //如果修改密码就密码加密
        String userPassword = user.getUserPassword();
        String encryptPassword;
        if(userPassword!= null && userPassword.length() > 0) {
            encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            updateUser.setUserPassword(encryptPassword);
        }

        //如果修改用户状态
        // 查询原来的用户状态,必须为2
        Integer originStatus = updateUser.getStatus();
        if(originStatus == 2){
            //刚注册,可以修改
            updateUser.setStatus(user.getStatus());
        }


        //修改用户信息
        boolean b = this.updateById(updateUser);
        if(!b){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"修改用户信息失败");
        }
        return this.getUserVOByUser(updateUser);
    }

    /**
     * 退出登录
     *
     * @param currentUserId
     * @return
     */
    @Override
    public boolean userLogout(Long currentUserId) {
        // 1. 从数据库中查询用户信息
        User user = this.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户不存在");
        }
        // 2. 删除登录用户的缓存
        redisTemplate.delete(LOGIN_USER_KEY + currentUserId);
        redisTemplate.delete(LOGIN_USER_TOKEN + currentUserId);

        return true;
    }



    /**
     * 通过用户的通话频道channel获取用户信息
     *
     * @param channel 通话频道
     * @return
     */
    @Override
    public User getByChannel(String channel) {
        if(channel == null || "".equals(channel)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户没有建立视频通话请求");
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("callChannel",channel);
        User user = this.getOne(userQueryWrapper);
        if(user == null){
            throw new BusinessException(ErrorCode.USER_ERROR,"用户没有建立视频通话请求");
        }
        return user;
    }


    /**
     * 用户信息统一返回脱敏
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVOByUser(User user) {
        if(user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        //根据家庭id查询家庭账号
        if(user.getFamilyId() != null && user.getFamilyId() > 0){
            Family family = familyMapper.selectById(user.getFamilyId());
            userVO.setFamilyAccount(family.getFamilyAccount());
        }
        //脱敏残疾人证功能
        String certificate = userVO.getUserCertificate();
        if(certificate != null && certificate.length() > 0){
            userVO.setUserCertificate("true");
        }else {
            userVO.setUserCertificate("false");
        }

        return userVO;
    }

    /**
     * 邮箱格式校验
     *@param userEmail 用户邮箱
     *  @return true与fasle
     */
    @Override
    public  boolean emailVerify(String userEmail) {
        //校验邮箱格式
        if (userEmail != null && !userEmail.trim().isEmpty() && userEmail.length()>0) {
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            Pattern pattern2 = Pattern.compile(emailRegex);
            Matcher matcher2 = pattern2.matcher(userEmail);
            if (!matcher2.matches()) {
                throw new BusinessException(ErrorCode.CHECK_FAILURE, "邮箱输入格式不正确");

            }
        }
        return true;
    }

    /**
     * 密码格式校验
     * @param userPassword 用户密码
     * @return true与fasle
     */
    @Override
    public  boolean passwordVerify(String userPassword) {
        if (userPassword == null || userPassword.trim().isEmpty() || userPassword.length()==0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码为空或全为空格");
        }
        // 密码格式校验（长度和字符组成一次性检查）
        String passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        Matcher matcher = pattern.matcher(userPassword);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码格式错误：必须包含字母和数字，长度至少8位");
        }
        return true;
    }

    /**
     * 手机号格式校验
     * @param userPhone 用户手机号
     * * @return true与fasle
     */
    @Override
    public  boolean phoneVerify(String userPhone) {
        // 2.手机号格式校验
        if (userPhone != null && userPhone.length() > 0 && !userPhone.trim().isEmpty()) {
            String phoneRegex = "^1[3-9]\\d{9}$";
            Pattern pattern = Pattern.compile(phoneRegex);
            Matcher matcher = pattern.matcher(userPhone);
            if (!matcher.matches()) {
                throw new BusinessException(ErrorCode.CHECK_FAILURE, "手机号输入格式不正确");
            }

        }
        return true;
    }

    /**
     * 残疾人证件号校验
     * @param certificate
     * @return
     */
    @Override
    public boolean certificateVerify(String certificate){
        if (certificate.length() == 20) {
            String certificateRegex = "^[0-9]+$";
            Pattern pattern = Pattern.compile(certificateRegex);
            Matcher matcher = pattern.matcher(certificate);
            if (!matcher.matches()) {
                throw new BusinessException(ErrorCode.CHECK_FAILURE, "证件输入格式不正确");
            }
            return true;

        }
        throw new BusinessException(ErrorCode.CHECK_FAILURE, "证件输入格式不正确");
    }

    /**
     * 将用户信息存入redis
     *  id 用户id
     */
    @Override
    public boolean setUserToRedis(Long id,String token){
        if(id == null){
            return false;
        }
        //将用户id存入redis中
        String loginUserKey = LOGIN_USER_KEY + id;
        //将用户token存入redis中
        String loginUserToken = LOGIN_USER_TOKEN + id;
        //设置过期时间为365天
        redisTemplate.opsForValue().set(loginUserKey, id, 365L, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(loginUserToken, token, 365L, TimeUnit.DAYS);
        return true;
    }

    /**
     * 雪花算法生成用户账号
     * @return 用户账号
     */
    @Override
    public String generateUserAccount(){
        // 2. 雪花算法生成账号，改进生成规则，确保高并发下唯一
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1); // 数据中心ID=1，机器ID=1
        long generatedId = idGenerator.nextId();

        // 增加时间戳或者其他唯一信息
        String userAccount = String.valueOf(generatedId).substring(0, 8);
        return userAccount;
    }

    /**
     * 登录的生成令牌,添加redis,返回LoginUserVO
     * @param user
     * @return
     */
    @Override
    public UserLoginVO getUserLoginVOByUser(User user){
        //生成令牌,加入redis
        String jwt = JWTUtils.generateJwt(user.getId().toString());
        setUserToRedis(user.getId(),jwt);
        //返回信息
        UserLoginVO res = new UserLoginVO();
        UserVO userVO = this.getUserVOByUser(user);
        res.setUser(userVO);
        res.setToken(jwt);
        return res;
    }

    /**
     * 校验令牌
     *
     * @param currentUserId
     * @param token
     * @return
     */
    @Override
    public UserVO testJwt(Long currentUserId, String token) {
        //检测token与redis的token是否一致且未过期
        token = StringUtils.remove(token,"Bearer ");
        String key = LOGIN_USER_TOKEN+currentUserId;
        if(redisTemplate.hasKey(key)){
            String redisToken = (String) redisTemplate.opsForValue().get(key);
            boolean b = JWTUtils.parseJWT(redisToken);
            //key不过期同时与传来的key相同
            if(!b || !redisToken.equals(token)){
                throw new BusinessException(ErrorCode.NOT_LOGIN,"令牌校验失败,请重新登录");
            }
        }else{
            throw new BusinessException(ErrorCode.NOT_LOGIN,"令牌校验失败,请重新登录");
        }
        //令牌校验成功
        User user = this.getById(currentUserId);
        log.info("当前用户id: {}", currentUserId);

        return this.getUserVOByUser(user);
    }

    /**
     * 通过通话频道获取正在通话的志愿者和盲人信息
     *
     * @param channel
     * @return
     */
    @Override
    public List<User> getUsersByChannel(String channel) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("callChannel",channel).eq("callStatus", UserCallStatusEnum.CALLING.getValue());
        return this.list(userQueryWrapper);
    }

    /**
     * 挂断通话修改双方的通话信息
     *
     * @param channel
     * @return
     */
    @Override
    public boolean updateCallUsersInformation(String channel) {
        //修改盲人与志愿者的信息
        List<User> users = this.getUsersByChannel(channel);
        if(users != null){
            users.forEach(user1 -> {
                user1.setCallStatus(UserCallStatusEnum.NO_CALLING.getValue());
                user1.setCallChannel("");
            });
            this.saveOrUpdateBatch(users);
        }
        return true;
    }

}
// [广告位招租]



