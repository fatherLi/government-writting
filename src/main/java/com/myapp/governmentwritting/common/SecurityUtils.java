package com.myapp.governmentwritting.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @description: 安全工具类，提供用户鉴权等辅助功能
 * @author: Leung Chiu Wai
 * @date: 2025-08-16 01:00:00
 * @version: 1.0
 */
public class SecurityUtils {

    /**
     * 获取当前登录的本地用户ID
     * @return userId，未登录或未找到则返回 null
     */
    /**
     * @description: 从当前Spring Security上下文的安全会话中提取已认证的本地用户ID
     * @author: Leung Chiu Wai
     * @date: 2025-09-05 23:37:52
     * @return: Long 当前登录用户的系统主键ID，如果尚未登录或会话失效则返回null
     */
    public static Long getCurrentUserId() {
        // 核心流程：通过 SecurityContextHolder 获取与当前执行线程绑定的认证信息对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Object localUserId = oAuth2User.getAttribute("localUserId");
            if (localUserId != null) {
                return Long.valueOf(localUserId.toString());
            }
        }
        return null;
    }

    /**
     * 判断当前用户是否是管理员
     */
    /**
     * @description: 判断当前登录的用户是否拥有系统管理员权限(ROLE_ADMIN)
     * @author: Leung Chiu Wai
     * @date: 2025-08-26 07:49:16
     * @return: boolean 若拥有管理员角色则返回 true，否则返回 false
     */
    public static boolean isAdmin() {
        // 核心流程：遍历当前用户的鉴权角色列表，匹配特定的系统管理员标识
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}
