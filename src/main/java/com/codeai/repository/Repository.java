package com.codeai.repository;

import java.util.Optional;

public interface Repository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    void deleteById(ID id);
}
