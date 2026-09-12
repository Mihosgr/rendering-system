package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Integer> {
    List<UserLoginHistory> findTop20ByUserIdOrderByLoginTimeDesc(Integer userId);
}