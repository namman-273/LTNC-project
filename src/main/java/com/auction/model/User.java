package com.auction.model;
public abstract class User extends Entity implements Observer {
        protected String username;
        private static final long serialVersionUID = 1L;
        public User(String id, String username) {
            super(id);
            this.username = username;
        }

        public String getUsername() { return username; }
        public void update(String message) {
        // Sau này chỗ này sẽ hiển thị lên màn hình JavaFX
           System.out.println("[NOTIFY - " + username + "]: " + message);
        }
}

