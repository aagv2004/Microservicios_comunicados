import { Injectable, signal } from '@angular/core';
import { User } from '../models/user';

interface Account {
  user: User;
  salt: Uint8Array<ArrayBuffer>;
  hash: ArrayBuffer;
}
interface Recovery {
  userId: string;
  expires: number;
}

// Simulación en memoria. Las cuentas y la sesión se reinician al recargar la página.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accounts: Account[] = [];
  private readonly recoveries = new Map<string, Recovery>();
  private readonly currentUser = signal<User | null>(null);
  readonly user = this.currentUser.asReadonly();
  private readonly ready = this.seed();

  private async seed(): Promise<void> {
    for (const [nombre, correo, rol] of [
      ['Usuario Demo', 'usuario@nexo.demo', 'USUARIO'],
      ['Admin Demo', 'admin@nexo.demo', 'ADMIN'],
    ] as const) {
      await this.addAccount(nombre, correo, 'NexoDemo2026!', rol);
    }
  }

  private async derive(password: string, salt: Uint8Array<ArrayBuffer>): Promise<ArrayBuffer> {
    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(password),
      'PBKDF2',
      false,
      ['deriveBits'],
    );
    return crypto.subtle.deriveBits(
      { name: 'PBKDF2', salt, iterations: 100000, hash: 'SHA-256' },
      key,
      256,
    );
  }

  private async addAccount(
    nombre: string,
    correo: string,
    password: string,
    rol: User['rol'],
  ): Promise<User> {
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const hash = await this.derive(password, salt);
    // Revalidar tras la operación asíncrona evita registros duplicados simultáneos.
    if (this.accounts.some((account) => account.user.correo === correo))
      throw new Error('Ya existe una cuenta con ese correo.');
    const user: User = {
      id: crypto.randomUUID(),
      nombre: nombre.trim(),
      correo,
      rol,
      biografia: '',
    };
    this.accounts.push({ user, salt, hash });
    return { ...user };
  }

  async register(nombre: string, correo: string, password: string): Promise<void> {
    await this.ready;
    const user = await this.addAccount(nombre, correo.trim().toLowerCase(), password, 'USUARIO');
    this.currentUser.set(user);
  }

  async login(correo: string, password: string): Promise<void> {
    await this.ready;
    const account = this.accounts.find((item) => item.user.correo === correo.trim().toLowerCase());
    if (!account) throw new Error('Correo o contraseña incorrectos.');
    const hash = new Uint8Array(await this.derive(password, account.salt));
    const expected = new Uint8Array(account.hash);
    if (!hash.every((value, index) => value === expected[index]))
      throw new Error('Correo o contraseña incorrectos.');
    this.currentUser.set({ ...account.user });
  }

  logout(): void {
    this.currentUser.set(null);
  }

  async requestRecovery(correo: string): Promise<string | null> {
    await this.ready;
    const account = this.accounts.find((item) => item.user.correo === correo.trim().toLowerCase());
    if (!account) return null;
    for (const [token, recovery] of this.recoveries) {
      if (recovery.userId === account.user.id || recovery.expires < Date.now())
        this.recoveries.delete(token);
    }
    const token = crypto.randomUUID();
    this.recoveries.set(token, { userId: account.user.id, expires: Date.now() + 15 * 60 * 1000 });
    return token;
  }

  async resetPassword(token: string, password: string): Promise<void> {
    await this.ready;
    const recovery = this.recoveries.get(token);
    if (!recovery || recovery.expires < Date.now()) {
      this.recoveries.delete(token);
      throw new Error('El enlace no es válido o venció. Solicita uno nuevo.');
    }
    const account = this.accounts.find((item) => item.user.id === recovery.userId);
    if (!account) throw new Error('El enlace no es válido.');
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const hash = await this.derive(password, salt);
    if (this.recoveries.get(token) !== recovery) throw new Error('El enlace ya fue utilizado.');
    account.salt = salt;
    account.hash = hash;
    this.recoveries.delete(token);
    if (this.user()?.id === account.user.id) this.logout();
  }

  updateProfile(nombre: string, correo: string, biografia: string): void {
    const current = this.user();
    if (!current) throw new Error('Se requiere una sesión activa.');
    const normalized = correo.trim().toLowerCase();
    if (
      this.accounts.some((item) => item.user.correo === normalized && item.user.id !== current.id)
    )
      throw new Error('Ese correo ya está registrado.');
    const account = this.accounts.find((item) => item.user.id === current.id)!;
    account.user = {
      ...account.user,
      nombre: nombre.trim(),
      correo: normalized,
      biografia: biografia.trim(),
    };
    this.currentUser.set({ ...account.user });
  }
}
