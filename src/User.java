public abstract class User extends Entity {
        protected String username;

        public User(String id, String username) {
            super(id);
            this.username = username;
        }

        public String getUsername() { return username; }
}

