import { Injectable } from '@angular/core';
import { Category, Topic } from '../models/forum';
// Datos locales: esta evaluación permite frontend y backend independientes.
@Injectable({ providedIn: 'root' })
export class ForumService {
  readonly categories: readonly Category[] = [
    { id: 'tecnologia', name: 'Tecnología', symbol: '⌘' },
    { id: 'videojuegos', name: 'Videojuegos', symbol: '✳' },
    { id: 'aprendizaje', name: 'Aprendizaje', symbol: '↗' },
    { id: 'creatividad', name: 'Creatividad', symbol: '◈' },
    { id: 'comunidad', name: 'Comunidad', symbol: '◎' },
  ];
  readonly topics: readonly Topic[] = [
    {
      id: 1,
      categoryId: 'tecnologia',
      title: '¿Qué herramienta cambió tu forma de programar?',
      excerpt:
        'Desde un atajo del editor hasta esa extensión que ya no puedes dejar de usar. Compartamos nuestros descubrimientos.',
      author: 'Camila Rojas',
      initials: 'CR',
      replies: 12,
    },
    {
      id: 2,
      categoryId: 'videojuegos',
      title: 'Jugar para mejorar: ¿cómo revisan sus partidas?',
      excerpt:
        'Estoy empezando a revisar mis repeticiones. ¿En qué se fijan primero para aprender sin perderse en los detalles?',
      author: 'Diego Muñoz',
      initials: 'DM',
      replies: 8,
    },
    {
      id: 3,
      categoryId: 'aprendizaje',
      title: 'Pequeños proyectos, grandes aprendizajes',
      excerpt:
        'Un espacio para mostrar lo que estamos construyendo y contar qué aprendimos en el camino.',
      author: 'Javiera Soto',
      initials: 'JS',
      replies: 6,
    },
    {
      id: 4,
      categoryId: 'creatividad',
      title: 'Inspiración para diseñar una interfaz más simple',
      excerpt:
        'Menos elementos, más intención. ¿Qué sitios o aplicaciones les inspiran cuando comienzan un diseño?',
      author: 'Matías Vera',
      initials: 'MV',
      replies: 4,
    },
    {
      id: 5,
      categoryId: 'comunidad',
      title: 'Preséntate: ¿qué te gustaría aprender este mes?',
      excerpt:
        'Cuéntanos un poco de ti, tus intereses y esa idea que tienes pendiente. Quizás encuentres con quién compartirla.',
      author: 'Antonia Díaz',
      initials: 'AD',
      replies: 15,
    },
  ];
}
