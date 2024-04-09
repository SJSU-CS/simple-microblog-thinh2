package edu.sjsu.cmpe272.simpleblog.server.repository;

import edu.sjsu.cmpe272.simpleblog.server.model.UserModel;
import org.apache.catalina.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserModel, Long> {
    Optional<UserModel> findByUsername(String username);
}
