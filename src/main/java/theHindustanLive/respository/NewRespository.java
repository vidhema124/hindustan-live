package theHindustanLive.respository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import theHindustanLive.entity.NewsEntity;

public interface NewRespository extends MongoRepository<NewsEntity, String> {

	@Aggregation(pipeline = { "{ '$match': { 'imageUrl': { '$ne': null, '$nin': [''] } } }",
			"{ '$sample': { 'size': 20 } }" })
	List<NewsEntity> findRandomNewsWithImage();

	@Aggregation(pipeline = {
	        "{ $match: { category: ?0, imageUrl: { $ne: null, $ne: '' } } }"
	    })
	    List<NewsEntity> findByCategory(String category);

	   @Aggregation(pipeline = {
		        "{ $match: { title: { $regex: ?0, $options: 'i' }, imageUrl: { $ne: null, $ne: '' } } }"
		    })
		    List<NewsEntity> findByTitleRegexWithImageUrl(String regex);
	@Query("{ 'imageUrl': { $ne: null, $nin: [''] } }")
	Page<NewsEntity> findAllWithImageUrl(Pageable page,Integer pageNumber);
	
	
	
	
	 Page<NewsEntity> findAllByOrderByTitleAsc(Pageable pageable);

	    Page<NewsEntity> findAllByOrderByTitleDesc(Pageable pageable);
	    
		Page<NewsEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

//		NewsEntity findByTitle(String title);

		NewsEntity findBySlug(String slug);

		NewsEntity findByTitle(String title);

}
