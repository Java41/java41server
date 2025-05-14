package org.acme;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    public static void add(String email, String password, String roles, String birthdate) {
        User user = new User();
        user.email = email;
        user.password = BcryptUtil.bcryptHash(password);
        user.roles = roles;
        user.birthdate = birthdate;
        user.persist();
    }

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }
}