<script lang="ts">
	import { onMount } from 'svelte';

	const API_BASE = 'http://localhost:8080/api';

	let isLoading = $state(false);
	let errorMessage = $state('');
	let isLoggedIn = $state(false);
	let backendConnected = $state(false);

	let user = $state({
		id: 'USR-01',
		name: 'Omkar',
		email: 'omkar@college.edu',
		rollNo: 'CY2024-042',
		ridesCount: 14,
		activeRental: null as null | { cycleId: string; startHubName: string; startTime: string }
	});

	let hubs = $state<Array<{ id: string; name: string; locationDescription: string; capacity: number; availableCycles: number }>>([
		{ id: 'HUB-LIB', name: 'Library Hub', locationDescription: 'Central Library Portico', capacity: 15, availableCycles: 8 },
		{ id: 'HUB-HOSTEL-B', name: 'Hostel Block B Hub', locationDescription: 'Hostel 2 Courtyard', capacity: 12, availableCycles: 5 },
		{ id: 'HUB-ENG-QUAD', name: 'Engineering Quad Hub', locationDescription: 'CS & Mechanical Quadrangle', capacity: 10, availableCycles: 2 }
	]);

	async function loadHubs() {
		try {
			const res = await fetch(`${API_BASE}/hubs`);
			if (res.ok) {
				const json = await res.json();
				if (json.success && json.data) {
					hubs = json.data;
					backendConnected = true;
				}
			}
		} catch {
			// Backend is offline or unreachable; retain local demo data
			backendConnected = false;
		}
	}

	onMount(() => {
		loadHubs();
	});

	async function handleGoogleLogin() {
		isLoading = true;
		errorMessage = '';

		try {
			const res = await fetch(`${API_BASE}/auth/google`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					email: 'omkar@college.edu',
					name: 'Omkar'
				})
			});

			if (res.ok) {
				const json = await res.json();
				if (json.success && json.data) {
					const data = json.data;
					user.id = data.id || 'USR-01';
					user.name = data.name || 'Omkar';
					user.email = data.email || 'omkar@college.edu';
					user.rollNo = data.rollNo || 'CY2024-042';
					user.ridesCount = data.ridesCount ?? 14;
					if (data.activeRental) {
						user.activeRental = {
							cycleId: data.activeRental.cycleId,
							startHubName: data.activeRental.startHubName || 'Campus Hub',
							startTime: new Date(data.activeRental.startTime).toLocaleTimeString()
						};
					} else {
						user.activeRental = null;
					}
					backendConnected = true;
					await loadHubs();
					isLoggedIn = true;
					isLoading = false;
					return;
				}
			}
		} catch {
			// Server offline: gracefully fallback to client state
		}

		// Fallback for offline development preview
		setTimeout(() => {
			isLoading = false;
			isLoggedIn = true;
		}, 600);
	}

	function handleLogout() {
		isLoggedIn = false;
	}

	async function handleRentDemo() {
		if (user.activeRental) {
			// Return bicycle
			try {
				const res = await fetch(`${API_BASE}/rentals/return`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						cycleId: user.activeRental.cycleId,
						hubId: 'HUB-LIB',
						userId: user.id
					})
				});

				if (res.ok) {
					const json = await res.json();
					if (json.success) {
						user.activeRental = null;
						user.ridesCount += 1;
						await loadHubs();
						return;
					}
				}
			} catch {
				// Server offline fallback
			}

			user.activeRental = null;
			user.ridesCount += 1;
		} else {
			// Unlock bicycle
			try {
				const res = await fetch(`${API_BASE}/rentals/unlock`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						cycleId: 'JC-108',
						userId: user.id
					})
				});

				if (res.ok) {
					const json = await res.json();
					if (json.success && json.data) {
						user.activeRental = {
							cycleId: json.data.cycleId,
							startHubName: json.data.startHubName || 'Library Station (Bay 4)',
							startTime: new Date(json.data.startTime).toLocaleTimeString()
						};
						await loadHubs();
						return;
					}
				}
			} catch {
				// Server offline fallback
			}

			user.activeRental = {
				cycleId: 'JC-108',
				startHubName: 'Library Station (Bay 4)',
				startTime: 'Just now'
			};
		}
	}
</script>

<svelte:head>
	<title>Jycle - Campus Cycle Sharing</title>
</svelte:head>

<main class="page-container">
	{#if !isLoggedIn}
		<div class="login-card">
			<!-- Brand Header -->
			<div class="brand">
				<div class="logo-circle">
					<svg class="bike-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="5.5" cy="17.5" r="3.5" />
						<circle cx="18.5" cy="17.5" r="3.5" />
						<path d="M15 6a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm-3 11.5L9 6H6" />
						<path d="M12 17.5V14l4.5-3.5L14 6" />
						<path d="m18 10-4.5 7.5" />
					</svg>
				</div>
				<h1 class="brand-title">Jycle</h1>
				<p class="brand-tagline">Campus Bicycle Sharing Network</p>
			</div>

			<!-- Divider -->
			<div class="card-divider"></div>

			<!-- Auth Section -->
			<div class="auth-section">
				<h2 class="login-heading">Login With Your College Mail</h2>
				<p class="login-subtext">
					Sign in with your official college Google account to reserve, unlock, and ride bicycles across campus.
				</p>

				{#if errorMessage}
					<div class="error-banner">
						{errorMessage}
					</div>
				{/if}

				<button
					class="google-login-btn"
					onclick={handleGoogleLogin}
					disabled={isLoading}
					aria-label="Sign in with College Google Account"
				>
					{#if isLoading}
						<span class="spinner"></span>
						<span>Authenticating...</span>
					{:else}
						<img
							src="/images/google-icon-logo-svgrepo-com.svg"
							alt="Google"
							class="google-icon"
						/>
						<span class="btn-text"><b>Sign in with Google</b></span>
					{/if}
				</button>

				<div class="domain-pill">
					<svg class="shield-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
					</svg>
					<span>Restricted to verified college domains</span>
				</div>
			</div>

			<!-- Campus Highlights -->
			<div class="features-grid">
				<div class="feature-item">
					<span class="feature-icon">⚡</span>
					<span class="feature-label">Instant QR Unlock</span>
				</div>
				<div class="feature-item">
					<span class="feature-icon">📍</span>
					<span class="feature-label">Multi-Hub Return</span>
				</div>
				<div class="feature-item">
					<span class="feature-icon">🌱</span>
					<span class="feature-label">Zero Emissions</span>
				</div>
			</div>

			<footer class="card-footer">
				<p>Need access or facing login issues? <a href="mailto:support@college.edu">Contact Campus IT</a></p>
			</footer>
		</div>
	{:else}
		<!-- Signed-In Student Dashboard Preview -->
		<div class="dashboard-card">
			<header class="dash-header">
				<div class="user-greeting">
					<div class="avatar">{user.name.charAt(0)}</div>
					<div>
						<h2>Welcome, {user.name}!</h2>
						<p class="user-email">{user.email} &bull; {user.rollNo}</p>
					</div>
				</div>
				<button class="logout-btn" onclick={handleLogout}>Sign Out</button>
			</header>

			<div class="dash-body">
				<!-- Server Connection Status Banner -->
				<div class="status-chip {backendConnected ? 'connected' : 'offline'}">
					<span class="dot"></span>
					<span>Backend API: {backendConnected ? 'Connected (Spring Boot)' : 'Local Preview'}</span>
				</div>

				<!-- Quick Stats -->
				<div class="stats-row">
					<div class="stat-box">
						<span class="stat-number">{user.ridesCount}</span>
						<span class="stat-title">Total Rides</span>
					</div>
					<div class="stat-box">
						<span class="stat-number">4.8 km</span>
						<span class="stat-title">Avg Distance</span>
					</div>
					<div class="stat-box">
						<span class="stat-number">{hubs.length}</span>
						<span class="stat-title">Campus Hubs</span>
					</div>
				</div>

				<!-- Rental Status -->
				<div class="rental-panel">
					<h3>Current Ride Status</h3>
					{#if user.activeRental}
						<div class="active-badge">
							<div class="status-indicator live"></div>
							<div>
								<strong>Cycle {user.activeRental.cycleId} in use</strong>
								<p class="rental-detail">Unlocked at {user.activeRental.startHubName} &bull; {user.activeRental.startTime}</p>
							</div>
						</div>
						<button class="action-btn return" onclick={handleRentDemo}>
							Return Bicycle at Nearest Hub
						</button>
					{:else}
						<p class="no-rental">You have no active rides right now.</p>
						<button class="action-btn unlock" onclick={handleRentDemo}>
							Scan QR to Unlock Bicycle
						</button>
					{/if}
				</div>

				<!-- Nearby Hubs Live Data -->
				<div class="hubs-preview">
					<h3>Campus Hubs</h3>
					<div class="hub-list">
						{#each hubs as hub}
							<div class="hub-item">
								<div>
									<div class="hub-name">{hub.name}</div>
									<div class="hub-desc">{hub.locationDescription}</div>
								</div>
								<span class="hub-count {hub.availableCycles > 3 ? 'available' : 'low'}">
									{hub.availableCycles} Cycles Available
								</span>
							</div>
						{/each}
					</div>
				</div>
			</div>
		</div>
	{/if}
</main>

<style>
	:global(body) {
		margin: 0;
		padding: 0;
		box-sizing: border-box;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
		background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 50%, #7dd3fc 100%);
		min-height: 100vh;
		color: #0f172a;
	}

	*, *::before, *::after {
		box-sizing: border-box;
	}

	.page-container {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1.5rem;
	}

	/* Card Base */
	.login-card,
	.dashboard-card {
		background-color: #ffffff;
		width: 100%;
		max-width: 440px;
		border-radius: 1.25rem;
		box-shadow: 0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.08);
		border: 1px solid rgba(226, 232, 240, 0.9);
		padding: 2.25rem 2rem;
		display: flex;
		flex-direction: column;
		transition: transform 0.2s ease, box-shadow 0.2s ease;
	}

	.dashboard-card {
		max-width: 520px;
	}

	/* Status Chip */
	.status-chip {
		display: inline-flex;
		align-items: center;
		gap: 0.45rem;
		font-size: 0.72rem;
		font-weight: 600;
		padding: 0.25rem 0.65rem;
		border-radius: 9999px;
		margin-bottom: 1.25rem;
	}

	.status-chip .dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
	}

	.status-chip.connected {
		background: #dcfce7;
		color: #15803d;
	}

	.status-chip.connected .dot {
		background: #22c55e;
	}

	.status-chip.offline {
		background: #f1f5f9;
		color: #64748b;
	}

	.status-chip.offline .dot {
		background: #94a3b8;
	}

	/* Brand Header */
	.brand {
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.logo-circle {
		width: 54px;
		height: 54px;
		border-radius: 50%;
		background: #0284c7;
		color: #ffffff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4px 12px rgba(2, 132, 199, 0.35);
		margin-bottom: 0.75rem;
	}

	.bike-icon {
		width: 30px;
		height: 30px;
	}

	.brand-title {
		margin: 0;
		font-size: 1.85rem;
		font-weight: 800;
		letter-spacing: -0.03em;
		color: #0369a1;
	}

	.brand-tagline {
		margin: 0.25rem 0 0;
		font-size: 0.875rem;
		color: #64748b;
		font-weight: 500;
	}

	.card-divider {
		height: 1px;
		background-color: #f1f5f9;
		margin: 1.5rem 0;
	}

	/* Auth Section */
	.auth-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		text-align: center;
	}

	.login-heading {
		margin: 0 0 0.5rem;
		font-size: 1.25rem;
		font-weight: 700;
		color: #0f172a;
	}

	.login-subtext {
		margin: 0 0 1.5rem;
		font-size: 0.875rem;
		line-height: 1.45;
		color: #64748b;
	}

	.error-banner {
		width: 100%;
		background-color: #fee2e2;
		color: #b91c1c;
		padding: 0.6rem 0.8rem;
		border-radius: 0.5rem;
		font-size: 0.85rem;
		margin-bottom: 1rem;
		text-align: center;
	}

	/* Google Login Button */
	.google-login-btn {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.75rem;
		background-color: #ffffff;
		border: 1.5px solid #cbd5e1;
		border-radius: 0.75rem;
		padding: 0.85rem 1.25rem;
		font-size: 0.95rem;
		color: #1e293b;
		cursor: pointer;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
		transition: all 0.2s ease;
		outline: none;
	}

	.google-login-btn:hover:not(:disabled) {
		background-color: #f8fafc;
		border-color: #94a3b8;
		box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
		transform: translateY(-1px);
	}

	.google-login-btn:active:not(:disabled) {
		transform: translateY(0);
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
	}

	.google-login-btn:disabled {
		opacity: 0.7;
		cursor: not-allowed;
	}

	.google-icon {
		width: 20px;
		height: 20px;
		flex-shrink: 0;
	}

	.btn-text {
		font-weight: 600;
	}

	/* Domain Info Pill */
	.domain-pill {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		margin-top: 1rem;
		background: #f0f9ff;
		color: #0369a1;
		padding: 0.35rem 0.75rem;
		border-radius: 9999px;
		font-size: 0.75rem;
		font-weight: 500;
	}

	.shield-icon {
		width: 14px;
		height: 14px;
	}

	/* Features Grid */
	.features-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 0.75rem;
		margin-top: 1.75rem;
		padding-top: 1.25rem;
		border-top: 1px solid #f1f5f9;
	}

	.feature-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		text-align: center;
		gap: 0.25rem;
	}

	.feature-icon {
		font-size: 1.25rem;
	}

	.feature-label {
		font-size: 0.72rem;
		font-weight: 600;
		color: #475569;
	}

	/* Footer */
	.card-footer {
		margin-top: 1.5rem;
		text-align: center;
		font-size: 0.78rem;
		color: #94a3b8;
	}

	.card-footer a {
		color: #0284c7;
		text-decoration: none;
		font-weight: 600;
	}

	.card-footer a:hover {
		text-decoration: underline;
	}

	/* Spinner */
	.spinner {
		width: 18px;
		height: 18px;
		border: 2px solid #e2e8f0;
		border-top-color: #0284c7;
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}

	/* Dashboard Styles */
	.dash-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 1.5rem;
		padding-bottom: 1rem;
		border-bottom: 1px solid #f1f5f9;
	}

	.user-greeting {
		display: flex;
		align-items: center;
		gap: 0.85rem;
	}

	.avatar {
		width: 44px;
		height: 44px;
		border-radius: 50%;
		background-color: #0284c7;
		color: white;
		font-weight: 700;
		font-size: 1.2rem;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.user-greeting h2 {
		margin: 0;
		font-size: 1.15rem;
		font-weight: 700;
		color: #0f172a;
	}

	.user-email {
		margin: 0.2rem 0 0;
		font-size: 0.8rem;
		color: #64748b;
	}

	.logout-btn {
		background: transparent;
		border: 1px solid #cbd5e1;
		border-radius: 0.5rem;
		padding: 0.4rem 0.75rem;
		font-size: 0.8rem;
		font-weight: 600;
		color: #64748b;
		cursor: pointer;
		transition: all 0.2s ease;
	}

	.logout-btn:hover {
		background: #f8fafc;
		color: #ef4444;
		border-color: #fca5a5;
	}

	/* Stats Row */
	.stats-row {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 0.75rem;
		margin-bottom: 1.5rem;
	}

	.stat-box {
		background: #f8fafc;
		border: 1px solid #e2e8f0;
		border-radius: 0.75rem;
		padding: 0.85rem 0.5rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		text-align: center;
	}

	.stat-number {
		font-size: 1.35rem;
		font-weight: 800;
		color: #0369a1;
	}

	.stat-title {
		font-size: 0.72rem;
		color: #64748b;
		margin-top: 0.25rem;
		font-weight: 600;
	}

	/* Rental Panel */
	.rental-panel {
		background: #f0f9ff;
		border: 1px solid #bae6fd;
		border-radius: 0.85rem;
		padding: 1.15rem;
		margin-bottom: 1.5rem;
	}

	.rental-panel h3 {
		margin: 0 0 0.75rem;
		font-size: 0.95rem;
		font-weight: 700;
		color: #0369a1;
	}

	.no-rental {
		font-size: 0.85rem;
		color: #475569;
		margin: 0 0 1rem;
	}

	.active-badge {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		background: #ffffff;
		border: 1px solid #e0f2fe;
		padding: 0.75rem;
		border-radius: 0.5rem;
		margin-bottom: 1rem;
	}

	.status-indicator.live {
		width: 10px;
		height: 10px;
		border-radius: 50%;
		background-color: #22c55e;
		box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.2);
	}

	.rental-detail {
		margin: 0.2rem 0 0;
		font-size: 0.75rem;
		color: #64748b;
	}

	.action-btn {
		width: 100%;
		padding: 0.75rem;
		border-radius: 0.6rem;
		font-size: 0.9rem;
		font-weight: 600;
		cursor: pointer;
		border: none;
		transition: background-color 0.2s ease;
	}

	.action-btn.unlock {
		background: #0284c7;
		color: white;
	}

	.action-btn.unlock:hover {
		background: #0369a1;
	}

	.action-btn.return {
		background: #059669;
		color: white;
	}

	.action-btn.return:hover {
		background: #047857;
	}

	/* Hubs */
	.hubs-preview h3 {
		margin: 0 0 0.75rem;
		font-size: 0.95rem;
		font-weight: 700;
		color: #1e293b;
	}

	.hub-list {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.hub-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0.65rem 0.75rem;
		border-radius: 0.5rem;
		background: #f8fafc;
		border: 1px solid #e2e8f0;
		font-size: 0.85rem;
	}

	.hub-name {
		font-weight: 600;
		color: #334155;
	}

	.hub-desc {
		font-size: 0.72rem;
		color: #94a3b8;
		margin-top: 0.15rem;
	}

	.hub-count {
		font-size: 0.75rem;
		font-weight: 600;
		padding: 0.2rem 0.5rem;
		border-radius: 9999px;
	}

	.hub-count.available {
		background: #dcfce7;
		color: #15803d;
	}

	.hub-count.low {
		background: #fef9c3;
		color: #a16207;
	}
</style>
