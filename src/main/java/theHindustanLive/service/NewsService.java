	package theHindustanLive.service;
	
	import java.util.List;
import java.util.Map;
import java.util.Optional;

import theHindustanLive.entity.NewsEntity;
	
	public interface NewsService {
	
	    void saveNews(List<NewsEntity> newsEntities);
		List<NewsEntity> getAllNews();
		
		String deleteNewsById(String id);
		List<NewsEntity> fetchNewsFromRSS(String categoryFilter);
		Optional<NewsEntity> newsGetById(String id);
		Optional<NewsEntity> findById(String id);
		NewsEntity saveNews(NewsEntity newsEntity);
		List<NewsEntity> getRandomNews();
		List<NewsEntity> getNewsByCategory(String category);
		List<Map<String, Object>> getNewsByCategory();
		List<NewsEntity> getNewsByTitle(String title);
	
	
	}
