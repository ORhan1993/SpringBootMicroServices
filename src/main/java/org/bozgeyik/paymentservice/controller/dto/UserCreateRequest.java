package org.bozgeyik.paymentservice.controller.dto;

// Getter, Setter, vb. için Lombok kullanılabilir veya manuel eklenebilir.
public class UserCreateRequest {

    private String name;
    private String email;
    private String password;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}