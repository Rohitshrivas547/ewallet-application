package com.example.majorproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;


@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    private final String REDIS_PREFIX_USER = "user::";
    private final String CREATE_WALLET_TOPIC = "create_wallet";



//    private final String REDIS_PREFIX_USER = "user::";

    public void createUser(UserRequest userRequest) {
//            it convert dto to entity---- userRequest is dto and user is entity
        User user = User.builder()
                .age(userRequest.getAge())
                .name(userRequest.getName())
                .email(userRequest.getEmail())
                .userName(userRequest.getUserName())
                .build();

        userRepository.save(user); // save in the db
        saveInCache(user); // save in cache

        //kafka
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username",user.getUserName());

        String message = jsonObject.toString();
        kafkaTemplate.send(CREATE_WALLET_TOPIC,message);

    }

    public void saveInCache(User user){  // cache only accept string and map
//        this way to  save in redis
        // you need to convert user to map
        Map map = objectMapper.convertValue(user, Map.class);
        redisTemplate.opsForHash().putAll(REDIS_PREFIX_USER+user.getUserName(),map);
        redisTemplate.expire(REDIS_PREFIX_USER+user.getUserName(), Duration.ofHours(12));
    }
    public User getUserByUserName(String userName) throws Exception{
//        try{
//            User user = userRepository.findByUserName(userName);
//            if(user == null){
//                throw new UserNotFoundException();
//            }
//            return user;
//        }catch (Exception e){
//            throw new UserNotFoundException();
//        }
        //logic
        // 1. first find in the redis cache
        Map map = redisTemplate.opsForHash().entries(REDIS_PREFIX_USER+userName);
//        User user = objectMapper.convertValue(map,User.class);
        // if not found in cache/map then search in db
        if(map==null || map.size()==0) {
            //cache miss --> search in DB
            User user = userRepository.findByUserName(userName);
            if(user!=null){
                saveInCache(user); // save in cache
            }
            else { //Throw an error
                throw new UserNotFoundException();
            }
            return user;
        }
        else{
            return objectMapper.convertValue(map,User.class); //objectMapper---> whatever present in the object converted in he map and viceversa
        }
    }

    public UserResponseDto findEmailAndNameDto(String userName){
        User user = userRepository.findByUserName(userName);
        UserResponseDto userResponseDto = UserResponseDto.builder().email(user.getEmail()).name(user.getName()).build();
        return userResponseDto;
    }
}
