package edu.sjsu.cmpe272.simpleblog.server.repository;

import edu.sjsu.cmpe272.simpleblog.server.model.MessageModel;
import org.aspectj.bridge.Message;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<MessageModel, Long> {
    MessageModel findFirstByOrderByIdDesc();
}
