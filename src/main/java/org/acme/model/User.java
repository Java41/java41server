package org.acme.model;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.util.Random;

@Entity
@Table(name = "app_users")
@UserDefinition
public class User extends PanacheEntity {

    @Username
    public String email;

    @Password
    public String password;

    @Roles
    public String roles;

    public String birthdate;

    @Column(unique = true)
    public String username;

    @Column(length = 50)
    public String firstName;

    @Column(length = 50)
    public String lastName;

    @Column(length = 255)
    public String photoUrl;

    @Column(nullable = false, columnDefinition = "boolean default true")
    public boolean active = true;

    public static void add(String email, String password, String roles, String birthdate, String firstName, String lastName, String photoUrl) {
        User user = new User();
        user.email = email;
        user.password = BcryptUtil.bcryptHash(password);
        user.roles = roles;
        user.birthdate = birthdate;
        user.username = generateUniqueUsername();
        user.firstName = firstName;
        user.lastName = lastName;
        user.photoUrl = photoUrl;
        user.active = true;
        user.persist();
    }

    public static User findByEmail(String email) {
        return find("email = ?1 and active = true", email).firstResult();
    }

    public static User findByUsername(String username) {
        return find("username = ?1 and active = true", username).firstResult();
    }

    public static User findById(Long id) {
        return find("id = ?1 and active = true", id).firstResult();
    }

    public void deactivate() {
        this.active = false;
        this.persist();
    }

    private static String generateUniqueUsername() {
        Random random = new Random();
        String username;
        do {
            username = "@User" + random.nextInt(1000000);
        } while (findByUsername(username) != null);
        return username;
    }
}