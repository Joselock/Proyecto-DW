package com.hospital.model.EntidadesJPA;

import java.io.Serializable;
import java.util.Objects;

public class UserId implements Serializable {
    private String username;
    private String email;

    public UserId() {}
    public UserId(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // equals y hashCode son OBLIGATORIOS para @IdClass
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId)) return false;
        UserId that = (UserId) o;
        return Objects.equals(username, that.username)
            && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }
}
