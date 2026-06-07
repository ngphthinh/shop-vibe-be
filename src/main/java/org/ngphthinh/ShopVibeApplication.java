package org.ngphthinh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ShopVibeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopVibeApplication.class, args);
	}

}
