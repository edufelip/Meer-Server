package com.edufelip.meer.domain.port;

public interface RateLimitPort {

  boolean allowCommentCreate(String userKey);

  boolean allowCommentEdit(String userKey);

  boolean allowLikeAction(String userKey);

  boolean allowSupportContact(String clientKey);
}
