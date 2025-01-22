package theHindustanLive.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import theHindustanLive.entity.NewsEntity;
import theHindustanLive.service.NewsService;

@AllArgsConstructor
@RestController
@RequestMapping("/news")
@CrossOrigin(origins = "http://localhost:3000")
public class NewsController {
	NewsService newsService;

	@GetMapping("/fetch-news")
	public ResponseEntity<List<NewsEntity>> fetchNews(@RequestParam(required = false) String category) {
		List<NewsEntity> response = newsService.fetchNewsFromRSS(category);
		if (category != null && !category.isEmpty()) {
			response = response.stream().filter(news -> category.equalsIgnoreCase(news.getCategory()))
					.collect(Collectors.toList());
		}

		return ResponseEntity.ok(response);
	}

	@PostMapping("/save-news")
	public ResponseEntity<String> saveNews(@RequestBody List<NewsEntity> newsEntities) {
		try {
			newsService.saveNews(newsEntities);
			return ResponseEntity.ok("News saved successfully!");
		} catch (Exception e) {
			return ResponseEntity.ok("Failed to save news: " + e.getMessage());
		}
	}

	@GetMapping("/get-all-news")
	public ResponseEntity<Map<String, Object>> getAllNews(
	        @RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
	        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
	    
	    Page<NewsEntity> pageResponse = newsService.getAllNews(pageNumber, pageSize);
	    Map<String, Object> response = new HashMap<>();
        response.put("news", pageResponse.getContent()); 
	    response.put("totalPages", pageResponse.getTotalPages()); 
	    response.put("currentPage", pageResponse.getNumber() + 1); 
	   
	    
	    return ResponseEntity.ok(response);
	}


	@DeleteMapping("/delete-news/{id}")
	public String deleteNews(@PathVariable String id) {
		return newsService.deleteNewsById(id);
	}

	@GetMapping("/getNewsById/{id}")
	public ResponseEntity<Optional<NewsEntity>> getById(@PathVariable String id) {
		Optional<NewsEntity> response = newsService.newsGetById(id);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/updateNewsById/{id}")
	public String updateProductById(@PathVariable("id") String id, @RequestBody NewsEntity newsEntity) {
		Optional<NewsEntity> emp = newsService.findById(id);
		if (emp.isPresent()) {
			newsEntity.setId(id);
			System.out.println("hello");

			newsService.saveNews(newsEntity);
			return "Product Details updated";
		} else {
			return "Product details do not exist";
		}
	}

	@GetMapping("/get-random-news")
	public ResponseEntity<List<NewsEntity>> getRandomNews() {
		List<NewsEntity> response = newsService.findRandomNewsWithImage();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/get-news-by-category")
	public ResponseEntity<List<NewsEntity>> getNewsByCategory(@RequestParam String category) {
		List<NewsEntity> response = newsService.getNewsByCategory(category);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getnews-by-category")
	public ResponseEntity<List<Map<String, Object>>> getNewsByCategory() {
		List<Map<String, Object>> response = newsService.getNewsByCategory();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/get-news-by-title")
	public ResponseEntity<List<NewsEntity>> getNewsByTitle(@RequestParam String title) {
	    List<NewsEntity> response = newsService.getNewsByTitle(title);
	    return ResponseEntity.ok(response);
	}
	@GetMapping("/testing/{id}")
	public ResponseEntity<Optional<NewsEntity>> getByIdTesting(@PathVariable String id) {
		Optional<NewsEntity> response = newsService.newsGetById(id);
		return ResponseEntity.ok(response);
	}
}
