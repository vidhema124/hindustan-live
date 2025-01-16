package theHindustanLive.respository;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import theHindustanLive.entity.NewsEntity;


public interface NewRespository extends MongoRepository<NewsEntity, String> {

	
	  @Aggregation(pipeline = {
		        "{ '$sample': { 'size': 20 } }"
		    })


			
		    List<NewsEntity> findRandomNews();
	  
	  List<NewsEntity> findByCategory(String category);
}
