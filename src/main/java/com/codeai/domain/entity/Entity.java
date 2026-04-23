package com.codeai.domain.entity;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class Entity<T> {

    protected T id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity<?> entity)) return false;
        return id != null && id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
