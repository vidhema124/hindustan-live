package theHindustanLive.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;

import theHindustanLive.entity.NewsEntity;

public interface NewsService {

	void saveNews(List<NewsEntity> newsEntities);
//		Map<String, Object> getAllNews(Integer pageNumber, Integer pageSize, String Order);

	String deleteNewsById(String id);

//	List<NewsEntity> fetchNewsFromRSS(String categoryFilter);

	Optional<NewsEntity> newsGetById(String id);

	Optional<NewsEntity> findById(String id);

	NewsEntity saveNews(NewsEntity newsEntity);

	List<NewsEntity> getNewsByCategory(String category);

	Map<String, List<Map<String, Object>>> getNewsByCategory();

	List<NewsEntity> getNewsByTitle(String title);

	List<NewsEntity> findRandomNewsWithImage();

	List<NewsEntity> getAllNews();

	Map<String, Object> getAllNews(Integer pageNumber, Integer pageSize);

	public NewsEntity getNewsBySlug(String slug);

	List<NewsEntity> fetchNewsFromRSS(List<String> categoryFilters);

	void saveSingleNews(NewsEntity newsEntity);
}
