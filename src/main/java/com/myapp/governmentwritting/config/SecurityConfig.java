package com.myapp.governmentwritting.config;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.entity.User;
import com.myapp.governmentwritting.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @description: Security配置类，用于项目配置
 * @author: Leung Chiu Wai
 * @date: 2025-09-12 19:05:39
 * @version: 1.0
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


    private final UserMapper userMapper;

    public SecurityConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * @description: 配置安全过滤链，定义拦截规则
     * @author: Leung Chiu Wai
     * @date: 2025-08-04 13:53:13
     * @param: http
     * @return: SecurityFilterChain
     */
    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Filter chain configured with enterprise security standards
        http
            .csrf().disable() // 企业内部系统通常可禁用或按需配置
            .cors().and()
            .authorizeRequests()
                .antMatchers("/api/public/**", "/login**").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2Login() // SSO单点登录
                .userInfoEndpoint()
                .userService(this.customOAuth2UserService()) // 核心：自定义用户信息加载逻辑
            .and()
            .and()
            // 企业级异常处理，前后端分离时，未登录或无权限应返回JSON而不是网页重定向
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write(JSONObject.toJSONString(Result.error(401, "未登录或登录已失效")));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write(JSONObject.toJSONString(Result.error(403, "没有访问该资源的权限")));
                });

        return http.build();
    }

    /**
     * 自定义 OAuth2 用户服务
     * 作用：在 SSO 登录成功后，根据 SSO 返回的用户名去本地数据库查询，如果存在则赋予其本地的角色（以使 @PreAuthorize 生效）
     */
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User oAuth2User = delegate.loadUser(request);
            
            // 认证中心返回的唯一标识，在 application.yml 中配置了 user-name-attribute: username
            String username = oAuth2User.getAttribute("username"); 
            if (username == null) {
                username = oAuth2User.getAttribute("sub"); // 备用 OIDC 规范的 sub
            }

            Collection<GrantedAuthority> mappedAuthorities = new ArrayList<>(oAuth2User.getAuthorities());

            Map<String, Object> customAttributes = new java.util.HashMap<>(oAuth2User.getAttributes());

            if (username != null) {
                LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
                query.eq(User::getUsername, username);
                User localUser = userMapper.selectOne(query);

                if (localUser != null) {
                    // 数据库里的角色如 "ADMIN", "USER"，配合 hasRole 使用需要加上 "ROLE_" 前缀
                    String roleName = localUser.getRole();
                    if (roleName != null && !roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    mappedAuthorities.add(new SimpleGrantedAuthority(roleName));
                    
                    // 将本地数据库的用户ID存入Attributes中，方便后续在Controller中获取
                    customAttributes.put("localUserId", localUser.getId());
                } else {
                    // 如果本地不存在，可以默认给个0或者抛出异常阻止登录
                    customAttributes.put("localUserId", 0L);
                }
            }

            String userNameAttributeName = request.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            
            return new DefaultOAuth2User(mappedAuthorities, customAttributes, userNameAttributeName);
        };
    }
}
