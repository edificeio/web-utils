/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.wseduc.webutils;

public abstract class Either<A, B> {

	public abstract boolean isLeft();
	public abstract boolean isRight();
	public abstract Left<A, B> left();
	public abstract Right<A, B> right();

	private Either() {}

	interface Function<T> {
		void apply(T x);
	}

	public final static class Left<L, R> extends Either<L, R>  {
		private final L leftValue;

		public Left(L l) {
			leftValue = l;
		}

		public boolean isRight() {
			return false;
		}

		public boolean isLeft() {
			return true;
		}

		public Right<L, R> right() {
			return null;
		}

		public Left<L, R> left() {
			return this;
		}

		public L getValue() {
			return leftValue;
		}
	}

	public final static class Right<L, R> extends Either<L, R>  {

		private final R rightValue;

		public Right(R r) {
			rightValue = r;
		}

		public boolean isRight() {
			return true;
		}

		public boolean isLeft() {
			return false;
		}

		public Right<L, R> right() {
			return this;
		}

		public Left<L, R> left() {
			return null;
		}

		public R getValue() {
			return rightValue;
		}
	}

	public void fold(Function<A> ifLeft, Function<B> ifRight) {
		if(isRight()) {
			ifRight.apply(right().getValue());
		} else {
			ifLeft.apply(left().getValue());
		}
	}

}
