package com.project.customer.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.customer.model.CityLookup;

@Repository
public interface CityRepo extends JpaRepository<CityLookup,Long> 
{
        boolean existsByCityNameIgnoreCase(String cityName);
}

  
