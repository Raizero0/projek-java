package com.javagear.models;

public class SessionManager {
    private static int currentUserId = -1;
    private static String currentUser;
    private static String userRole;
    private static String currentUserName;
    private static String currentUserEmail;

    private static String currentUserFoto = "default.png";

    public static void setCurrentUserId(int id) {
        currentUserId = id;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUser(String username, String role) {
        currentUser = username;
        userRole = role;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static void setCurrentUserName(String name) {
        currentUserName = name;
    }

    public static String getCurrentUserName() {
        return currentUserName;
    }

    public static void setCurrentUserEmail(String email) {
        currentUserEmail = email;
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public static void setCurrentUserFoto(String foto) {
        currentUserFoto = (foto == null || foto.isEmpty()) ? "default.png" : foto;
    }

    public static String getCurrentUserFoto() {
        return currentUserFoto;
    }

    public static void clearSession() {
        currentUserId = -1;
        currentUser = null;
        userRole = null;
        currentUserName = null;
        currentUserEmail = null;
        currentUserFoto = "default.png"; // Reset ke default
        System.out.println("✅ Session cleared");
    }

    public static boolean isLoggedIn() {
        return currentUserId != -1 && currentUser != null;
    }

    public static boolean isAdmin() {
        return "admin".equals(userRole);
    }

    public static boolean isUser() {
        return "user".equals(userRole);
    }

    public static String getSessionInfo() {
        return String.format("Session Info: UserID=%d, User=%s, Role=%s, Foto=%s", currentUserId,
                currentUser, userRole, currentUserFoto);
    }

    public static boolean validateSession() {
        if (!isLoggedIn())
            return false;
        if (userRole == null || (!userRole.equals("admin") && !userRole.equals("user")))
            return false;
        return true;
    }
}
