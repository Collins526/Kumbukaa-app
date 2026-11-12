const app = {
    token: localStorage.getItem('kumbuka_token'),
    user: null,
    baseUrl: '/api',

    init() {
        try {
            const userData = localStorage.getItem('kumbuka_user');
            this.user = userData ? JSON.parse(userData) : null;
        } catch (error) {
            console.error('Failed to parse saved admin session', error);
            this.clearSession();
        }

        document.getElementById('login-form').addEventListener('submit', (event) => {
            void this.login(event);
        });
        document.getElementById('logout-button').addEventListener('click', () => this.logout());
        document.getElementById('refresh-users').addEventListener('click', () => {
            void this.fetchUsers();
        });

        if (this.token && this.user) {
            this.showDashboard();
        } else {
            this.showLogin();
        }
    },

    async login(event) {
        event.preventDefault();
        const email = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        const errorEl = document.getElementById('auth-error');
        const loginButton = document.querySelector('.login-button');

        errorEl.hidden = true;
        loginButton.disabled = true;
        loginButton.textContent = 'Signing in…';

        try {
            const response = await fetch(`${this.baseUrl}/auth/admin/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await response.json();

            if (!response.ok || !data.token) {
                throw new Error(data.message || 'Administrator sign-in failed.');
            }

            this.token = data.token;
            this.user = data;
            localStorage.setItem('kumbuka_token', data.token);
            localStorage.setItem('kumbuka_user', JSON.stringify(data));
            document.getElementById('login-form').reset();
            this.showDashboard();
        } catch (error) {
            errorEl.textContent = error instanceof Error ? error.message : 'Could not sign in.';
            errorEl.hidden = false;
        } finally {
            loginButton.disabled = false;
            loginButton.innerHTML = 'Continue to admin <span aria-hidden="true">→</span>';
        }
    },

    showLogin(message) {
        document.getElementById('dashboard').hidden = true;
        document.getElementById('auth-section').hidden = false;
        if (message) {
            const errorEl = document.getElementById('auth-error');
            errorEl.textContent = message;
            errorEl.hidden = false;
        }
    },

    showDashboard() {
        document.getElementById('auth-section').hidden = true;
        document.getElementById('dashboard').hidden = false;
        const email = this.user && this.user.email ? this.user.email : 'Administrator';
        const name = this.user && this.user.name ? this.user.name.trim() : '';
        const displayName = name || email;
        document.getElementById('user-display').textContent = displayName;
        document.getElementById('admin-name').textContent = name || 'Administrator';
        document.querySelector('.admin-avatar').textContent = displayName.charAt(0).toUpperCase();
        void this.fetchUsers();
    },

    clearSession() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('kumbuka_token');
        localStorage.removeItem('kumbuka_user');
    },

    logout() {
        this.clearSession();
        this.showLogin();
    },

    async fetchUsers() {
        const errorEl = document.getElementById('data-error');
        const table = document.getElementById('users-table');
        const refreshButton = document.getElementById('refresh-users');
        errorEl.hidden = true;
        refreshButton.disabled = true;
        table.replaceChildren(this.messageRow('Loading accounts…'));

        try {
            const response = await fetch(`${this.baseUrl}/admin/users`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });

            if (response.status === 401 || response.status === 403) {
                this.clearSession();
                this.showLogin('Your administrator session has expired or does not have admin access. Please sign in again.');
                return;
            }
            if (!response.ok) {
                throw new Error(`Could not load accounts (HTTP ${response.status}).`);
            }

            const users = await response.json();
            if (!Array.isArray(users)) {
                throw new Error('The server returned an invalid account list.');
            }
            this.renderUsers(users);
        } catch (error) {
            table.replaceChildren(this.messageRow('Account data could not be loaded.'));
            errorEl.textContent = error instanceof Error ? error.message : 'Could not load account data.';
            errorEl.hidden = false;
        } finally {
            refreshButton.disabled = false;
        }
    },

    renderUsers(users) {
        const table = document.getElementById('users-table');
        const loanCount = users.reduce((sum, user) => sum + Number(user.loansLent || 0) + Number(user.loansBorrowed || 0), 0);
        const activeCount = users.filter((user) => Number(user.loansLent || 0) + Number(user.loansBorrowed || 0) > 0).length;

        document.getElementById('stat-users').textContent = users.length.toLocaleString();
        document.getElementById('stat-loans').textContent = loanCount.toLocaleString();
        document.getElementById('stat-active').textContent = activeCount.toLocaleString();
        document.getElementById('user-count').textContent = `${users.length.toLocaleString()} ${users.length === 1 ? 'account' : 'accounts'}`;

        if (users.length === 0) {
            table.replaceChildren(this.messageRow('No user accounts found.'));
            return;
        }

        const rows = users.map((user) => {
            const row = document.createElement('tr');
            const accountCell = document.createElement('td');
            const account = document.createElement('div');
            const avatar = document.createElement('span');
            const name = document.createElement('span');
            account.className = 'account-cell';
            avatar.className = 'account-avatar';
            avatar.textContent = String(user.fullName || user.email || '?').trim().charAt(0).toUpperCase();
            name.className = 'account-name';
            name.textContent = user.fullName || 'Unnamed account';
            account.append(avatar, name);
            accountCell.append(account);
            row.append(
                accountCell,
                this.textCell(user.email || '—'),
                this.textCell(user.phoneNumber || '—'),
                this.countCell(user.loansLent),
                this.countCell(user.loansBorrowed)
            );
            return row;
        });
        table.replaceChildren(...rows);
    },

    textCell(value) {
        const cell = document.createElement('td');
        cell.textContent = value;
        return cell;
    },

    countCell(value) {
        const cell = document.createElement('td');
        const count = document.createElement('span');
        count.className = 'activity-count';
        count.textContent = Number(value || 0).toLocaleString();
        cell.append(count);
        return cell;
    },

    messageRow(message) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.className = 'table-message';
        cell.colSpan = 5;
        cell.textContent = message;
        row.append(cell);
        return row;
    }
};

app.init();
