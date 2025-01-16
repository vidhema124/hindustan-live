package theHindustanLive.respository;

import org.springframework.data.mongodb.repository.MongoRepository;

import theHindustanLive.entity.CategoryEntity;

public interface CategoryRespository extends MongoRepository<CategoryEntity,String> {

}
